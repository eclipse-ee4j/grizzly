/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.impl.FutureImpl;

public final class ClientCheckFilter extends BaseFilter {
    private final FutureImpl<Boolean> future;
    private final int packetsNumber;
    private final int size;

    private final int[] packetsCounter = new int[10];

    private final AtomicInteger counter = new AtomicInteger();

    public ClientCheckFilter(FutureImpl<Boolean> future, int packetsNumber, int size) {
        this.future = future;
        this.packetsNumber = packetsNumber;
        this.size = size;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {

        final String message = ctx.getMessage();

        try {
            assertEquals(size, message.length());

            final char[] charsToCompare = new char[size];
            Arrays.fill(charsToCompare, message.charAt(0));
            final String stringToCompare = new String(charsToCompare);

            assertEquals(stringToCompare, message);

            final int index = message.charAt(0) - '0';
            packetsCounter[index]++;

            if (counter.incrementAndGet() >= packetsNumber) {
                int receivedPackets = 0;
                for (int i = 0; i < 10; i++) {
                    receivedPackets += packetsCounter[i];
                }

                assertEquals(packetsNumber, receivedPackets);
                future.result(true);
            }

        } catch (Throwable t) {
            future.failure(t);
        }
        return ctx.getStopAction();
    }
}
