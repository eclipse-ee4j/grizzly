/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpResponsePacket;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;

public class WebSocketClientFilter extends BaseWebSocketFilter {
    private static final Logger LOGGER = Grizzly.logger(WebSocketClientFilter.class);

    // ----------------------------------------------------- Methods from Filter
    
    /**
     * Method handles Grizzly {@link Connection} connect phase. Check if the {@link Connection} is a client-side {@link
     * WebSocket}, if yes - creates websocket handshake packet and send it to a server. Otherwise, if it's not websocket
     * connection - pass processing to the next {@link Filter} in a chain.
     *
     * @param ctx {@link FilterChainContext}
     *
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * 
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        LOGGER.log(Level.FINEST, "handleConnect");
        // Get connection
        final Connection connection = ctx.getConnection();
        // check if it's websocket connection
        if (!webSocketInProgress(connection)) {
            // if not - pass processing to a next filter
            return ctx.getInvokeAction();
        }

        WebSocketHolder.get(connection).handshake.initiate(ctx);
        // call the next filter in the chain
        return ctx.getInvokeAction();
    }
    
    // ---------------------------------------- Methods from BaseWebSocketFilter


    @Override
    protected NextAction handleHandshake(FilterChainContext ctx, HttpContent content) throws IOException {
        return handleClientHandShake(ctx, content);
    }


    // --------------------------------------------------------- Private Methods


    private static NextAction handleClientHandShake(FilterChainContext ctx, HttpContent content) {
        final WebSocketHolder holder = WebSocketHolder.get(ctx.getConnection());
        holder.handshake.validateServerResponse((HttpResponsePacket) content.getHttpHeader());
        holder.webSocket.onConnect();
        
        if (content.getContent().hasRemaining()) {
            return ctx.getRerunFilterAction();
        } else {
            content.recycle();
            return ctx.getStopAction();
        }
    }
}
