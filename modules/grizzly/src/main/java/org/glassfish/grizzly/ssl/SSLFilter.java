/*
 * Copyright (c) 2008, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.ssl;

import static org.glassfish.grizzly.ssl.SSLUtils.isHandshaking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.FileTransfer;
import org.glassfish.grizzly.GenericCloseListener;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.PendingWriteQueueLimitExceededException;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainContext.Operation;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.utils.Exceptions;

/**
 * SSL {@link Filter} to operate with SSL encrypted data.
 *
 * @author Alexey Stashok
 */
public class SSLFilter extends SSLBaseFilter {
    private static final Logger LOGGER = Grizzly.logger(SSLFilter.class);

    private final Attribute<SSLHandshakeContext> handshakeContextAttr;
    private final SSLEngineConfigurator clientSSLEngineConfigurator;

    private final ConnectionCloseListener closeListener = new ConnectionCloseListener();

    // Max bytes SSLFilter may enqueue
    protected volatile int maxPendingBytes = Integer.MAX_VALUE;

    // ------------------------------------------------------------ Constructors

    public SSLFilter() {
        this(null, null);
    }

    /**
     * Build <tt>SSLFilter</tt> with the given {@link SSLEngineConfigurator}.
     *
     * @param serverSSLEngineConfigurator SSLEngine configurator for server side connections
     * @param clientSSLEngineConfigurator SSLEngine configurator for client side connections
     */
    public SSLFilter(SSLEngineConfigurator serverSSLEngineConfigurator, SSLEngineConfigurator clientSSLEngineConfigurator) {
        this(serverSSLEngineConfigurator, clientSSLEngineConfigurator, true);
    }

    /**
     * Build <tt>SSLFilter</tt> with the given {@link SSLEngineConfigurator}.
     *
     * @param serverSSLEngineConfigurator SSLEngine configurator for server side connections
     * @param clientSSLEngineConfigurator SSLEngine configurator for client side connections
     * @param renegotiateOnClientAuthWant <tt>true</tt>, if SSLBaseFilter has to force client authentication during
     * re-handshake, in case the client didn't send its credentials during the initial handshake in response to
     * "wantClientAuth" flag. In this case "needClientAuth" flag will be raised and re-handshake will be initiated
     */
    public SSLFilter(SSLEngineConfigurator serverSSLEngineConfigurator, SSLEngineConfigurator clientSSLEngineConfigurator,
            boolean renegotiateOnClientAuthWant) {

        super(serverSSLEngineConfigurator, renegotiateOnClientAuthWant);

        if (clientSSLEngineConfigurator == null) {
            this.clientSSLEngineConfigurator = new SSLEngineConfigurator(SSLContextConfigurator.DEFAULT_CONFIG.createSSLContext(true), true, false, false);
        } else {
            this.clientSSLEngineConfigurator = clientSSLEngineConfigurator;
        }

        handshakeContextAttr = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("SSLFilter-SSLHandshakeContextAttr");
    }

    /**
     * @return {@link SSLEngineConfigurator} used by the filter to create new {@link SSLEngine} for client-side
     * {@link Connection}s
     */
    public SSLEngineConfigurator getClientSSLEngineConfigurator() {
        return clientSSLEngineConfigurator;
    }

    // ----------------------------------------------------- Methods from Filter

    @Override
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();

        if (ctx.getMessage() instanceof FileTransfer) {
            throw new IllegalStateException("TLS operations not supported with SendFile messages");
        }

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (connection) {
            final SSLConnectionContext sslCtx = obtainSslConnectionContext(connection);

            final SSLEngine sslEngine = sslCtx.getSslEngine();
            if (sslEngine != null && !isHandshaking(sslEngine)) {
                return sslCtx.isServerMode() ? super.handleWrite(ctx) : accurateWrite(ctx, true);
            } else {
                if (sslEngine == null || !handshakeContextAttr.isSet(connection)) {
                    handshake(connection, null, null, clientSSLEngineConfigurator, ctx, false);
                }

                return accurateWrite(ctx, false);
            }
        }
    }

    // ---------------------------------------------------------- Public Methods

    /**
     * @return the maximum number of bytes that may be queued to be written to a particular {@link Connection}. This value
     * is related to the situation when we try to send application data before SSL handshake completes, so the data should
     * be stored and sent on wire once handshake will be completed.
     */
    public int getMaxPendingBytesPerConnection() {
        return maxPendingBytes;
    }

    /**
     * Configures the maximum number of bytes that may be queued to be written for a particular {@link Connection}. This
     * value is related to the situation when we try to send application data before SSL handshake completes, so the data
     * should be stored and sent on wire once handshake will be completed.
     *
     * @param maxPendingBytes maximum number of bytes that may be queued to be written for a particular {@link Connection}
     */
    public void setMaxPendingBytesPerConnection(final int maxPendingBytes) {
        this.maxPendingBytes = maxPendingBytes;
    }

    public void handshake(final Connection connection, final CompletionHandler<SSLEngine> completionHandler) throws IOException {
        handshake(connection, completionHandler, null, clientSSLEngineConfigurator);
    }

    public void handshake(final Connection connection, final CompletionHandler<SSLEngine> completionHandler, final Object dstAddress) throws IOException {
        handshake(connection, completionHandler, dstAddress, clientSSLEngineConfigurator);
    }

    public void handshake(final Connection connection, final CompletionHandler<SSLEngine> completionHandler, final Object dstAddress,
            final SSLEngineConfigurator sslEngineConfigurator) throws IOException {
        handshake(connection, completionHandler, dstAddress, sslEngineConfigurator, createContext(connection, Operation.WRITE), true);
    }

    protected void handshake(final Connection<?> connection, final CompletionHandler<SSLEngine> completionHandler, final Object dstAddress,
            final SSLEngineConfigurator sslEngineConfigurator, final FilterChainContext context, final boolean forceBeginHandshake) throws IOException {
        final SSLConnectionContext sslCtx = obtainSslConnectionContext(connection);
        SSLEngine sslEngine = sslCtx.getSslEngine();

        if (sslEngine == null) {
            sslEngine = createClientSSLEngine(sslCtx, sslEngineConfigurator);

            sslCtx.configure(sslEngine);
        } else if (!isHandshaking(sslEngine)) { // if handshake haven't been started
            sslEngineConfigurator.configure(sslEngine);
        }

        notifyHandshakeStart(connection);

        // if the session is still valid - we're most probably
        // tearing down the SSL connection and we can't do beginHandshake(),
        // because it will throw an exception
        if (forceBeginHandshake || !sslEngine.getSession().isValid()) {
            sslEngine.beginHandshake();
        }

        handshakeContextAttr.set(connection, new SSLHandshakeContext(connection, completionHandler));
        connection.addCloseListener(closeListener);

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (connection) {
            final Buffer buffer = doHandshakeStep(sslCtx, context, null);
            assert buffer == null;
        }
    }

    // --------------------------------------------------------- Private Methods

    /**
     * Has to be called in synchronized(connection) {...} block.
     */
    private NextAction accurateWrite(final FilterChainContext ctx, final boolean isHandshakeComplete) throws IOException {

        final Connection connection = ctx.getConnection();
        SSLHandshakeContext handshakeContext = handshakeContextAttr.get(connection);

        if (isHandshakeComplete && handshakeContext == null) {
            return super.handleWrite(ctx);
        } else {
            if (handshakeContext == null) {
                handshakeContext = new SSLHandshakeContext(connection, null);
                handshakeContextAttr.set(connection, handshakeContext);
            }

            if (!handshakeContext.add(ctx)) {
                return super.handleWrite(ctx);
            }
        }

        return ctx.getSuspendAction();
    }

    @Override
    protected void notifyHandshakeComplete(final Connection<?> connection, final SSLEngine sslEngine) {

        final SSLHandshakeContext handshakeContext = handshakeContextAttr.get(connection);
        if (handshakeContext != null) {
            connection.removeCloseListener(closeListener);
            handshakeContext.completed(sslEngine);
            handshakeContextAttr.remove(connection);
        }

        super.notifyHandshakeComplete(connection, sslEngine);
    }

    @Override
    protected void notifyHandshakeFailed(Connection connection, Throwable t) {
        final SSLHandshakeContext handshakeContext = handshakeContextAttr.get(connection);
        if (handshakeContext != null) {
            connection.removeCloseListener(closeListener);
            handshakeContext.failed(t);
        }

        super.notifyHandshakeFailed(connection, t);
    }

    @Override
    protected Buffer doHandshakeStep(final SSLConnectionContext sslCtx, final FilterChainContext ctx, final Buffer inputBuffer, final Buffer tmpAppBuffer0)
            throws IOException {
        try {
            return super.doHandshakeStep(sslCtx, ctx, inputBuffer, tmpAppBuffer0);
        } catch (IOException ioe) {
            SSLHandshakeContext context = handshakeContextAttr.get(ctx.getConnection());
            if (context != null) {
                context.failed(ioe);
            }
            throw ioe;
        }
    }

    protected SSLEngine createClientSSLEngine(final SSLConnectionContext sslCtx, final SSLEngineConfigurator sslEngineConfigurator) {

        return sslEngineConfigurator.createSSLEngine(HostNameResolver.getPeerHostName(sslCtx.getConnection()), -1);
    }

    // ----------------------------------------------------------- Inner Classes

    private final class SSLHandshakeContext {

        private CompletionHandler<SSLEngine> completionHandler;

        private final Connection connection;
        private List<FilterChainContext> pendingWriteContexts;
        private int sizeInBytes = 0;

        private Throwable error;
        private boolean isComplete;

        public SSLHandshakeContext(final Connection connection, final CompletionHandler<SSLEngine> completionHandler) {
            this.connection = connection;
            this.completionHandler = completionHandler;
        }

        /**
         * Has to be called in synchronized(connection) {...} scope.
         */
        public boolean add(FilterChainContext context) throws IOException {
            if (error != null) {
                throw Exceptions.makeIOException(error);
            }
            if (isComplete) {
                return false;
            }

            final Buffer buffer = context.getMessage();

            final int newSize = sizeInBytes + buffer.remaining();
            if (newSize > maxPendingBytes) {
                throw new PendingWriteQueueLimitExceededException("Max queued data limit exceeded: " + newSize + '>' + maxPendingBytes);
            }

            sizeInBytes = newSize;

            if (pendingWriteContexts == null) {
                pendingWriteContexts = new LinkedList<>();
            }

            pendingWriteContexts.add(context);

            return true;
        }

        public void completed(final SSLEngine engine) {
            try {
                synchronized (connection) {
                    isComplete = true;

                    final CompletionHandler<SSLEngine> completionHandlerLocal = completionHandler;
                    completionHandler = null;

                    if (completionHandlerLocal != null) {
                        completionHandlerLocal.completed(engine);
                    }

                    resumePendingWrites();
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Unexpected SSLHandshakeContext.completed() error", e);
                failed(e);
            }
        }

        public void failed(final Throwable throwable) {
            synchronized (connection) {
                if (error != null) {
                    return;
                }

                error = throwable;

                final CompletionHandler<SSLEngine> completionHandlerLocal = completionHandler;
                completionHandler = null;

                if (completionHandlerLocal != null) {
                    completionHandlerLocal.failed(throwable);
                }

                connection.closeWithReason(Exceptions.makeIOException(throwable));

                // pending writes will fail
                resumePendingWrites();
            }
        }

        private void resumePendingWrites() {
            final List<FilterChainContext> pendingWriteContextsLocal = pendingWriteContexts;
            pendingWriteContexts = null;

            if (pendingWriteContextsLocal != null) {
                for (FilterChainContext ctx : pendingWriteContextsLocal) {
                    try {
                        ctx.resume();
                    } catch (Exception e) {

                    }
                }

                pendingWriteContextsLocal.clear();
                sizeInBytes = 0;
            }
        }
    }

    /**
     * Close listener, which is used to notify handshake completion handler about failure, if <tt>Connection</tt> will be
     * unexpectedly closed.
     */
    private final class ConnectionCloseListener implements GenericCloseListener {
        @Override
        public void onClosed(final Closeable closeable, final CloseType type) throws IOException {
            final Connection connection = (Connection) closeable;
            final SSLHandshakeContext handshakeContext = handshakeContextAttr.get(connection);
            if (handshakeContext != null) {
                handshakeContext.failed(new java.io.EOFException());
                handshakeContextAttr.remove(connection);
            }
        }
    }

    /**
     * The static class is used as JDK 1.7+ guard and shouldn't be initialized by JDK 1.6.x at runtime, because one of the
     * methods is only available since 1.7.
     */
    private static class HostNameResolver {

        public static String getPeerHostName(final Connection<?> connection) {
            // try to get the peer's host name we try to connect to
            final Object addr = connection.getPeerAddress();
            return addr instanceof InetSocketAddress ? ((InetSocketAddress) addr).getHostString() : // supported in 1.7+
                    null;
        }
    }
}
