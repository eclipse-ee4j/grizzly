/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Filter which pauses protocol chain processing for a certain time.
 *
 * @author Alexey Stashok
 */
public class DelayFilter extends BaseFilter {

    private final long readTimeoutMillis;
    private final long writeTimeoutMillis;

    public DelayFilter(long readTimeoutMillis, long writeTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
        this.writeTimeoutMillis = writeTimeoutMillis;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        try {
            Thread.sleep(readTimeoutMillis);
        } catch (Exception ignored) {
        }

        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        try {
            Thread.sleep(writeTimeoutMillis);
        } catch (Exception ignored) {
        }

        return ctx.getInvokeAction();
    }
}
