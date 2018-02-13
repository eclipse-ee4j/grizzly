/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.connectionpool;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * The client {@link org.glassfish.grizzly.filterchain.Filter} responsible for
 * tracking the number of established client connections and passing server
 * responses to the {@link ClientCallback} for appropriate accounting.
 */
public class ClientFilter extends BaseFilter {
    private final ClientCallback callback;

    public ClientFilter(ClientCallback callback) {
        this.callback = callback;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        // new Connection is established - notify the callback
        callback.onConnectionEstablished(ctx.getConnection());
        return ctx.getStopAction();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        // a response is received - pass it to the callback
        final String responseMessage = ctx.getMessage();
        callback.onResponseReceived(ctx.getConnection(), responseMessage);
        
        return ctx.getStopAction();
    }
}
