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

import java.util.Arrays;

import org.glassfish.grizzly.Buffer;

final class IntegerWriter {

    private static final byte NEW = 0x0;
    private static final byte CONFIGURED = 0x1;
    private static final byte FIRST_BYTE_WRITTEN = 0x2;
    private static final byte DONE = 0x4;

    private byte state = NEW;

    private int payload;
    private int N;
    private int value;

    //
    // 0 1 2 3 4 5 6 7
    // +---+---+---+---+---+---+---+---+
    // | | | | | | | | |
    // +---+---+---+-------------------+
    // |<--------->|<----------------->|
    // payload N=5
    //
    // payload is the contents of the left-hand side part of the octet;
    // it is truncated to fit into 8-N bits, where 1 <= N <= 8;
    //
    public IntegerWriter configure(int value, int N, int payload) {
        if (state != NEW) {
            throw new IllegalStateException("Already configured");
        }
        if (value < 0) {
            throw new IllegalArgumentException("value >= 0: value=" + value);
        }
        checkPrefix(N);
        this.value = value;
        this.N = N;
        this.payload = payload & 0xFF & 0xFFFFFFFF << N;
        state = CONFIGURED;
        return this;
    }

    public boolean write(Buffer output) {
        if (state == NEW) {
            throw new IllegalStateException("Configure first");
        }
        if (state == DONE) {
            return true;
        }

        if (!output.hasRemaining()) {
            return false;
        }
        if (state == CONFIGURED) {
            int max = (2 << N - 1) - 1;
            if (value < max) {
                output.put((byte) (payload | value));
                state = DONE;
                return true;
            }
            output.put((byte) (payload | max));
            value -= max;
            state = FIRST_BYTE_WRITTEN;
        }
        if (state == FIRST_BYTE_WRITTEN) {
            while (value >= 128 && output.hasRemaining()) {
                output.put((byte) (value % 128 + 128));
                value /= 128;
            }
            if (!output.hasRemaining()) {
                return false;
            }
            output.put((byte) value);
            state = DONE;
            return true;
        }
        throw new InternalError(Arrays.toString(new Object[] { state, payload, N, value }));
    }

    private static void checkPrefix(int N) {
        if (N < 1 || N > 8) {
            throw new IllegalArgumentException("1 <= N <= 8: N= " + N);
        }
    }

    public IntegerWriter reset() {
        state = NEW;
        return this;
    }
}
