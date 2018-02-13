/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpServerFilter;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.utils.IdleTimeoutFilter;

/**
 * WebSocket {@link Filter} implementation, which supposed to be placed into a {@link FilterChain} right after HTTP
 * Filter: {@link HttpServerFilter}, {@link HttpClientFilter}; depending whether it's server or client side. The
 * <tt>BaseWebSocketFilter</tt> handles websocket connection, handshake phases and, when receives a websocket frame -
 * redirects it to appropriate connection ({@link WebSocketApplication}, {@link WebSocket}) for processing.
 *
 * @author Alexey Stashok
 */
public abstract class BaseWebSocketFilter extends BaseFilter {
    
    private static final Logger LOGGER = Grizzly.logger(BaseWebSocketFilter.class);
    private static final long DEFAULT_WS_IDLE_TIMEOUT_IN_SECONDS = 15 * 60;
    private final long wsTimeoutMS;
    
    
    // ------------------------------------------------------------ Constructors


    /**
     * Constructs a new <code>BaseWebSocketFilter</code> with a default idle connection
     * timeout of 15 minutes;
     */
    public BaseWebSocketFilter()  {
        this(DEFAULT_WS_IDLE_TIMEOUT_IN_SECONDS);
    }

    /**
     * Constructs a new <code>BaseWebSocketFilter</code> with a default idle connection
     * timeout of 15 minutes;
     */
    public BaseWebSocketFilter(final long wsTimeoutInSeconds) {
        if (wsTimeoutInSeconds <= 0) {
            this.wsTimeoutMS = IdleTimeoutFilter.FOREVER;
        } else {
            this.wsTimeoutMS = wsTimeoutInSeconds * 1000;
        }
    }
    
    // ----------------------------------------------------- Methods from Filter
    /**
     * Method handles Grizzly {@link Connection} close phase. Check if the {@link Connection} is a {@link WebSocket}, if
     * yes - tries to close the websocket gracefully (sending close frame) and calls {@link
     * WebSocket#onClose(DataFrame)}. If the Grizzly {@link Connection} is not websocket - passes processing to the next
     * filter in the chain.
     *
     * @param ctx {@link FilterChainContext}
     *
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     *
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        // Get the Connection
        final Connection connection = ctx.getConnection();
        // check if Connection has associated WebSocket (is websocket)
        if (webSocketInProgress(connection)) {
            // if yes - get websocket
            final WebSocket ws = getWebSocket(connection);
            if (ws != null) {
                // if there is associated websocket object (which means handshake was passed)
                // close it gracefully
                ws.close();
            }
        }
        return ctx.getInvokeAction();
    }

    /**
     * Handle Grizzly {@link Connection} read phase. If the {@link Connection} has associated {@link WebSocket} object
     * (websocket connection), we check if websocket handshake has been completed for this connection, if not -
     * initiate/validate handshake. If handshake has been completed - parse websocket {@link DataFrame}s one by one and
     * pass processing to appropriate {@link WebSocket}: {@link WebSocketApplication} for server- and client- side
     * connections.
     *
     * @param ctx {@link FilterChainContext}
     *
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * 
     * @throws java.io.IOException
     */
    @Override
    @SuppressWarnings("unchecked")
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        // Get the Grizzly Connection
        final Connection connection = ctx.getConnection();
        // Get the parsed HttpContent (we assume prev. filter was HTTP)
        final HttpContent message = ctx.getMessage();
        // Get the HTTP header
        final HttpHeader header = message.getHttpHeader();
        // Try to obtain associated WebSocket
        final WebSocketHolder holder = WebSocketHolder.get(connection);
        WebSocket ws = getWebSocket(connection);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "handleRead websocket: {0} content-size={1} headers=\n{2}",
                new Object[]{ws, message.getContent().remaining(), header});
        }
        if (ws == null || !ws.isConnected()) {
            // If websocket is null - it means either non-websocket Connection, or websocket with incomplete handshake
            if (!webSocketInProgress(connection) &&
                !"websocket".equalsIgnoreCase(header.getUpgrade())) {
                // if it's not a websocket connection - pass the processing to the next filter
                return ctx.getInvokeAction();
            }
            
            try {
                // Handle handshake
                return handleHandshake(ctx, message);
            } catch (HandshakeException e) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "Handshake error. Code: {0} Msg:{1}",
                        new Object[]{e.getCode(), e.getMessage()});
                }

                onHandshakeFailure(connection, e);
            }

            // Handshake error
            return ctx.getStopAction();
        }
        // this is websocket with the completed handshake
        if (message.getContent().hasRemaining()) {
            // get the frame(s) content

            Buffer buffer = message.getContent();
            message.recycle();
            // check if we're currently parsing a frame
            try {
                while (buffer != null && buffer.hasRemaining()) {
                    if (holder.buffer != null) {
                        buffer = Buffers.appendBuffers(
                                ctx.getMemoryManager(), holder.buffer, buffer);
                        
                        holder.buffer = null;
                    }
                    final DataFrame result = holder.handler.unframe(buffer);
                    if (result == null) {
                        holder.buffer = buffer;
                        break;
                    } else {
                        result.respond(holder.webSocket);
                    }
                }
            } catch (FramingException e) {
                holder.webSocket.onClose(new ClosingFrame(e.getClosingCode(), e.getMessage()));
            } catch (Exception wse) {
                if (holder.application.onError(holder.webSocket, wse)) {
                    holder.webSocket.onClose(new ClosingFrame(1011, wse.getMessage()));
                }
            }
        }
        return ctx.getStopAction();
    }

    /**
     * Handle Grizzly {@link Connection} write phase. If the {@link Connection} has associated {@link WebSocket} object
     * (websocket connection), we assume that message is websocket {@link DataFrame} and serialize it into a {@link
     * Buffer}.
     *
     * @param ctx {@link FilterChainContext}
     *
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     *
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        // get the associated websocket
        final WebSocket websocket = getWebSocket(ctx.getConnection());
        final Object msg = ctx.getMessage();
        // if there is one
        if (websocket != null && DataFrame.isDataFrame(msg)) {
            final DataFrame frame = (DataFrame) msg;
            final WebSocketHolder holder = WebSocketHolder.get(ctx.getConnection());
            final Buffer wrap = Buffers.wrap(ctx.getMemoryManager(), holder.handler.frame(frame));
            ctx.setMessage(wrap);
        }
        // invoke next filter in the chain
        return ctx.getInvokeAction();
    }
    
    
    // --------------------------------------------------------- Private Methods

    /**
     * Handle websocket handshake
     *
     * @param ctx {@link FilterChainContext}
     * @param content HTTP message
     *
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     *
     * @throws java.io.IOException
     */
    protected abstract NextAction handleHandshake(FilterChainContext ctx, HttpContent content)
    throws IOException;


    /**
     * The method is called when WebSocket handshake fails for the {@link Connection}.
     * 
     * @param connection
     * @param e 
     */
    protected void onHandshakeFailure(final Connection connection,
            final HandshakeException e) {
    }


    private static WebSocket getWebSocket(final Connection connection) {
        return WebSocketHolder.getWebSocket(connection);
    }

    protected static boolean webSocketInProgress(final Connection connection) {
        return WebSocketHolder.isWebSocketInProgress(connection);
    }
    
    protected void setIdleTimeout(final FilterChainContext ctx) {
        final FilterChain filterChain = ctx.getFilterChain();
        if (filterChain.indexOfType(IdleTimeoutFilter.class) >= 0) {
            IdleTimeoutFilter.setCustomTimeout(ctx.getConnection(),
                    wsTimeoutMS, TimeUnit.MILLISECONDS);
        }
    }
}
