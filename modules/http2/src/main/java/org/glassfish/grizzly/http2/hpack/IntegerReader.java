/*
 * Copyright (c) 2016, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.grizzly.http2.hpack;

import static java.lang.String.format;

import java.util.Arrays;

import org.glassfish.grizzly.Buffer;

final class IntegerReader {

    private static final byte NEW = 0x0;
    private static final byte CONFIGURED = 0x1;
    private static final byte FIRST_BYTE_READ = 0x2;
    private static final byte DONE = 0x4;

    private byte state = NEW;

    private int N;
    private int maxValue;
    private int value;
    private long r;
    private long b = 1;

    public IntegerReader configure(int N) {
        return configure(N, Integer.MAX_VALUE);
    }

    //
    // Why is it important to configure 'maxValue' here. After all we can wait
    // for the integer to be fully read and then check it. Can't we?
    //
    // Two reasons.
    //
    // 1. Value wraps around long won't be unnoticed.
    // 2. It can spit out an exception as soon as it becomes clear there's
    // an overflow. Therefore, no need to wait for the value to be fully read.
    //
    public IntegerReader configure(int N, int maxValue) {
        if (state != NEW) {
            throw new IllegalStateException("Already configured");
        }
        checkPrefix(N);
        if (maxValue < 0) {
            throw new IllegalArgumentException("maxValue >= 0: maxValue=" + maxValue);
        }
        this.maxValue = maxValue;
        this.N = N;
        state = CONFIGURED;
        return this;
    }

    public boolean read(Buffer input) {
        if (state == NEW) {
            throw new IllegalStateException("Configure first");
        }
        if (state == DONE) {
            return true;
        }
        if (!input.hasRemaining()) {
            return false;
        }
        if (state == CONFIGURED) {
            int max = (2 << N - 1) - 1;
            int n = input.get() & max;
            if (n != max) {
                value = n;
                state = DONE;
                return true;
            } else {
                r = max;
            }
            state = FIRST_BYTE_READ;
        }
        if (state == FIRST_BYTE_READ) {
            // variable-length quantity (VLQ)
            byte i;
            do {
                if (!input.hasRemaining()) {
                    return false;
                }
                i = input.get();
                long increment = b * (i & 127);
                if (r + increment > maxValue) {
                    throw new IllegalArgumentException(format("Integer overflow: maxValue=%,d, value=%,d", maxValue, r + increment));
                }
                r += increment;
                b *= 128;
            } while ((128 & i) == 128);

            value = (int) r;
            state = DONE;
            return true;
        }
        throw new InternalError(Arrays.toString(new Object[] { state, N, maxValue, value, r, b }));
    }

    public int get() throws IllegalStateException {
        if (state != DONE) {
            throw new IllegalStateException("Has not been fully read yet");
        }
        return value;
    }

    private static void checkPrefix(int N) {
        if (N < 1 || N > 8) {
            throw new IllegalArgumentException("1 <= N <= 8: N= " + N);
        }
    }

    public IntegerReader reset() {
        b = 1;
        state = NEW;
        return this;
    }
}
