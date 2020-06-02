/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;

public class WebSocketFilter extends BaseWebSocketFilter {

    // ------------------------------------------------------------ Constructors

    public WebSocketFilter() {
        super();
    }

    public WebSocketFilter(long wsTimeoutInSeconds) {
        super(wsTimeoutInSeconds);
    }

    // ---------------------------------------- Methods from BaseWebSocketFilter

    @Override
    protected NextAction handleHandshake(FilterChainContext ctx, HttpContent content) throws IOException {
        return handleServerHandshake(ctx, content);
    }

    // --------------------------------------------------------- Private Methods

    /**
     * Handle server-side websocket handshake
     *
     * @param ctx {@link FilterChainContext}
     * @param requestContent HTTP message
     * @throws {@link IOException}
     */
    private NextAction handleServerHandshake(final FilterChainContext ctx, final HttpContent requestContent) throws IOException {

        // get HTTP request headers
        final HttpRequestPacket request = (HttpRequestPacket) requestContent.getHttpHeader();
        try {
            if (doServerUpgrade(ctx, requestContent)) {
                return ctx.getInvokeAction(); // not a WS request, pass to the next filter.
            }
            setIdleTimeout(ctx);
        } catch (HandshakeException e) {
            ctx.write(composeHandshakeError(request, e));
            throw e;
        }
        requestContent.recycle();

        return ctx.getStopAction();

    }

    protected boolean doServerUpgrade(final FilterChainContext ctx, final HttpContent requestContent) throws IOException {
        return !WebSocketEngine.getEngine().upgrade(ctx, requestContent);
    }

    private static HttpResponsePacket composeHandshakeError(final HttpRequestPacket request, final HandshakeException e) {
        final HttpResponsePacket response = request.getResponse();
        response.setStatus(e.getCode());
        response.setReasonPhrase(e.getMessage());
        return response;
    }
}
