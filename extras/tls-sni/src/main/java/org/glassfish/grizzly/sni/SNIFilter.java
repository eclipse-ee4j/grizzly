/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.sni;

import static org.glassfish.grizzly.ssl.SSLUtils.allocateInputBuffer;
import static org.glassfish.grizzly.ssl.SSLUtils.allowDispose;
import static org.glassfish.grizzly.ssl.SSLUtils.getSslConnectionContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.ssl.SSLConnectionContext;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.utils.Charsets;

/**
 * TLS Server Name Indication (SNI) {@link Filter} implementation. This filter supports SNI extension on both client and
 * server sides, however the client side logic works on JDK 7+ only.
 *
 * On the <tt>server-side</tt> this filter allows developers to set custom {@link SSLEngineConfigurator}, based on the
 * host name provided by the client in the SSL CLIENT_HELLO message. An {@link SNIServerConfigResolver} registered via
 * {@link #setServerSSLConfigResolver(org.glassfish.grizzly.sni.SNIServerConfigResolver)} would be responsible for
 * customizing {@link SSLEngineConfigurator}.
 *
 * On the other hand for client-side it's not mandatory to register {@link SNIClientConfigResolver}, because the host
 * name information could be obtained from the {@link Connection#getPeerAddress()}. However
 * {@link SNIClientConfigResolver} could be used to customize the host name.
 *
 * @author Alexey Stashok
 */
public class SNIFilter extends SSLFilter {
    private static final Logger LOGGER = Grizzly.logger(SNIFilter.class);

    private static final byte HANDSHAKE_TYPE = 0x16;
    private static final int MIN_TLS_VERSION = 0x0301;
    private static final int SSLV3_RECORD_HEADER_SIZE = 5; // SSLv3 record header
    private static final int CLIENT_HELLO_HST = 0x1;

    private SNIServerConfigResolver serverResolver;
    private SNIClientConfigResolver clientResolver;

    public SNIFilter() {
    }

    /**
     * Construct an <tt>SNIFilter</tt> with the given default client and server side {@link SSLEngineConfigurator}.
     *
     * @param serverSSLEngineConfigurator
     * @param clientSSLEngineConfigurator
     */
    public SNIFilter(final SSLEngineConfigurator serverSSLEngineConfigurator, final SSLEngineConfigurator clientSSLEngineConfigurator) {
        super(serverSSLEngineConfigurator, clientSSLEngineConfigurator);
    }

    /**
     * Construct an <tt>SNIFilter</tt> with the given default {@link SSLEngineConfigurator}.
     *
     * @param serverSSLEngineConfigurator SSLEngine configurator for server side connections
     * @param clientSSLEngineConfigurator SSLEngine configurator for client side connections
     * @param renegotiateOnClientAuthWant
     */
    public SNIFilter(final SSLEngineConfigurator serverSSLEngineConfigurator, final SSLEngineConfigurator clientSSLEngineConfigurator,
            final boolean renegotiateOnClientAuthWant) {
        super(serverSSLEngineConfigurator, clientSSLEngineConfigurator, renegotiateOnClientAuthWant);
    }

    /**
     * @return {@link SNIServerConfigResolver}, which is responsible for customizing {@link SSLEngineConfigurator} for newly
     * accepted {@link Connection}s, based on SNI host name information sent by a client
     */
    public SNIServerConfigResolver getServerSSLConfigResolver() {
        return serverResolver;
    }

    /**
     * Sets {@link SNIServerConfigResolver}, which is responsible for customizing {@link SSLEngineConfigurator} for newly
     * accepted {@link Connection}s, based on SNI host name information sent by a client.
     *
     * @param resolver {@link SNIServerConfigResolver}
     */
    public void setServerSSLConfigResolver(final SNIServerConfigResolver resolver) {
        this.serverResolver = resolver;
    }

    /**
     * @return {@link SNIClientConfigResolver}, which is responsible for customizing {@link SSLEngineConfigurator} and SNI
     * host name to be sent to a server
     */
    public SNIClientConfigResolver getClientSSLConfigResolver() {
        return clientResolver;
    }

    /**
     * Sets {@link SNIClientConfigResolver}, which is responsible for customizing {@link SSLEngineConfigurator} and SNI host
     * name to be sent to a server.
     *
     * @param resolver
     */
    public void setClientSSLConfigResolver(final SNIClientConfigResolver resolver) {
        this.clientResolver = resolver;
    }

    @Override
    protected SSLTransportFilterWrapper createOptimizedTransportFilter(final TransportFilter childFilter) {
        // return SNI aware TransportFilterWrapper, because original one
        // can create default SSLEngine instance, which is not needed
        return new SNIAwareTransportFilterWrapper(childFilter, this);
    }

    @Override
    public NextAction handleConnect(final FilterChainContext ctx) throws IOException {
        // new client-side connection
        final Connection c = ctx.getConnection();

        final String host;
        final SSLEngineConfigurator configurator;

        final SNIClientConfigResolver resolver = this.clientResolver;
        if (resolver != null) {
            // if there's a client resolver associated - use it to get
            // the SNI host name and SSLEngineConfigurator
            final SNIConfig sniConfig = resolver.resolve(c);
            if (sniConfig != null && !sniConfig.isClientConfig) {
                throw new IllegalStateException("SNIConfig has to represent client config, not a server one");
            }

            host = sniConfig != null ? sniConfig.host : null;
            configurator = sniConfig != null && sniConfig.sslEngineConfigurator != null ? sniConfig.sslEngineConfigurator : getClientSSLEngineConfigurator();

        } else {
            // if resolver is not set - try to set default SNI host, based
            // on Connection's peer address
            configurator = getClientSSLEngineConfigurator();
            final Object addr = c.getPeerAddress();
            host = addr instanceof InetSocketAddress ? ((InetSocketAddress) addr).getHostString() : null;
        }

        final SSLConnectionContext sslCtx = obtainSslConnectionContext(ctx.getConnection());

        final SSLEngine sslEngine = host != null ? configurator.createSSLEngine(host, -1) : configurator.createSSLEngine();

        sslCtx.configure(sslEngine);
        sslEngine.beginHandshake();
        notifyHandshakeStart(c);

        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final SNIServerConfigResolver localResolver = serverResolver;

        // if resolver is not registered - use the default config
        if (localResolver == null) {
            return super.handleRead(ctx);
        }

        final Connection c = ctx.getConnection();

        if (getSslConnectionContext(c) == null) {
            // if SSLConnectionContext is null - it means it's the first message
            // sent from the client and hopefully it's CLIENT_HELLO
            final Buffer input = ctx.getMessage();
            /*
             * SSLv2 length field is in bytes 0/1 SSLv3/TLS length field is in bytes 3/4
             */
            if (input.remaining() < 5) {
                return ctx.getStopAction(input);
            }

            int pos = input.position();
            final byte byte0 = input.get(pos++);
            final byte byte1 = input.get(pos++);
            final byte byte2 = input.get(pos++);

            if (checkTlsVersion(byte0, byte1, byte2)) {
                final byte byte3 = input.get(pos++);
                final byte byte4 = input.get(pos);

                /*
                 * One of the SSLv3/TLS message types.
                 */
                final int len = ((byte3 & 0xff) << 8) + (byte4 & 0xff) + SSLV3_RECORD_HEADER_SIZE;

                if (input.remaining() < len) {
                    // incomplete chunk
                    return ctx.getStopAction(input);
                }

                // extract SNI host name
                final String hostName = getHostName(input, len);
                final SNIConfig sniConfig = localResolver.resolve(c, hostName);

                if (sniConfig != null && sniConfig.isClientConfig) {
                    throw new IllegalStateException("SNIConfig has to represent server config, not a client one");
                }

                final SSLEngineConfigurator configurator = sniConfig != null && sniConfig.sslEngineConfigurator != null ? sniConfig.sslEngineConfigurator
                        : getServerSSLEngineConfigurator();

                final SSLConnectionContext sslCtx = obtainSslConnectionContext(c);

                final SSLEngine sslEngine = configurator.createSSLEngine();
                sslCtx.configure(sslEngine);
                sslEngine.beginHandshake();
                notifyHandshakeStart(c);
            }
        }

        return super.handleRead(ctx);
    }

    private String getHostName(final Buffer input, final int len) {
        int current = SSLV3_RECORD_HEADER_SIZE;

        final int handshakeType = input.get(current++);

        // Check Handshake
        if (handshakeType != CLIENT_HELLO_HST) {
            return null;
        }

        // Skip over another length
        current += 3;
        // Skip over protocolversion
        current += 2;
        // Skip over random number
        current += 4 + 28;

        // Skip over session ID
        final int sessionIDLength = input.get(current++) & 0xFF;
        current += sessionIDLength;

        final int cipherSuiteLength = input.getShort(current) & 0xFFFF;
        current += 2;
        current += cipherSuiteLength;

        final int compressionMethodLength = input.get(current++) & 0xFF;
        current += compressionMethodLength;

        final int absLen = input.position() + len;
        if (current >= absLen) {
            return null;
        }

        // Skip over extensionsLength
        current += 2;

        while (current < absLen) {
            final int extensionType = input.getShort(current) & 0xFFFF;
            current += 2;

            final int extensionDataLength = input.getShort(current) & 0xFFFF;
            current += 2;

            if (extensionType == 0) {

                final int namesCount = input.getShort(current) & 0xFFFF;
                current += 2;

                for (int i = 0; i < namesCount; i++) {

                    final int nameType = input.get(current++) & 0xFF;

                    final int nameLen = input.getShort(current) & 0xFFFF;
                    current += 2;

                    if (nameType == 0) {
                        return input.toStringContent(Charsets.ASCII_CHARSET, current, current + nameLen);
                    }

                    current += nameLen;
                }
            }

            current += extensionDataLength;
        }

        return null;
    }

    static boolean checkTlsVersion(final byte byte0, final byte major, final byte minor) {

        return byte0 == HANDSHAKE_TYPE && (major << 8 | minor & 0xff) >= MIN_TLS_VERSION;
    }

    private static final class SNIAwareTransportFilterWrapper extends SSLTransportFilterWrapper {

        public SNIAwareTransportFilterWrapper(final TransportFilter transportFilter, final SSLBaseFilter sslBaseFilter) {
            super(transportFilter, sslBaseFilter);
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final Connection connection = ctx.getConnection();
            final SSLConnectionContext sslCtx = getSslConnectionContext(connection);

            if (sslCtx != null && sslCtx.getSslEngine() != null) {
                ctx.setMessage(allowDispose(allocateInputBuffer(sslCtx)));
            }

            return wrappedFilter.handleRead(ctx);
        }
    }
}
