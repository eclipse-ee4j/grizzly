/*
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.portunif;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.SocketConnectorHandler;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOConnectorHandler;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.portunif.finders.SSLProtocolFinder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.utils.StringDecoder;
import org.glassfish.grizzly.utils.StringFilter;
import org.junit.Test;

/**
 * Port-unification test, which involves secured and plain protocols. Test creates a protocol tree: PUFilter |
 * ------------------------- | | | | X Y Z SSLFilter | PUFilter -------------------- | | | A B X
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class SSLAndPlainTest {
    public static final int PORT = 17401;
    public static final Charset CHARSET = Charset.forName("ISO-8859-1");

    @Test
    public void sslFinderFirst() throws Exception {
//        final String[] plainProtocols = {"X", "Y", "Z"};
//        final String[] sslProtocols = {"A", "B", "X"};

        // Protocol name should be 5 bytes min to let SSLFinder (which is run first) recognize the protocol.
        final ProtocolDescription[] protocols = new ProtocolDescription[] { new ProtocolDescription("XXXXX", false), new ProtocolDescription("AAAAA", true),
                new ProtocolDescription("YYYYY", false), new ProtocolDescription("BBBBB", true), new ProtocolDescription("ZZZZZ", false),
                new ProtocolDescription("XXXXX", true) };

        SSLContextConfigurator sslContextConfigurator = createSSLContextConfigurator();
        SSLEngineConfigurator clientSSLEngineConfigurator = null;
        SSLEngineConfigurator serverSSLEngineConfigurator = null;

        if (sslContextConfigurator.validateConfiguration(true)) {
            clientSSLEngineConfigurator = new SSLEngineConfigurator(sslContextConfigurator.createSSLContext());
            serverSSLEngineConfigurator = new SSLEngineConfigurator(sslContextConfigurator.createSSLContext(), false, false, false);
        } else {
            fail("Failed to validate SSLContextConfiguration.");
        }

        Connection connection = null;

        final PUFilter rootPuFilter = new PUFilter();

        // Configure SSL PUFilter, which will be set as child of the root PUFilter

        final PUFilter sslPuFilter = new PUFilter();
        final FilterChain sslProtocolFilterChain = rootPuFilter.getPUFilterChainBuilder()
                .add(new SSLFilter(serverSSLEngineConfigurator, clientSSLEngineConfigurator)).add(sslPuFilter).build();

        // Register SSL Finder and SSL PU FilterChain
        rootPuFilter.register(new SSLProtocolFinder(serverSSLEngineConfigurator), sslProtocolFilterChain);

        for (final ProtocolDescription protocol : protocols) {
            if (protocol.isSecure) {
                sslPuFilter.register(createProtocol(sslPuFilter, protocol));
            } else {
                rootPuFilter.register(createProtocol(rootPuFilter, protocol));
            }
        }

        final FilterChainBuilder puFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter()).add(rootPuFilter);

        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        transport.setProcessor(puFilterChainBuilder.build());

        try {
            transport.bind(PORT);
            transport.start();

            for (final ProtocolDescription protocol : protocols) {
                final FutureImpl<Boolean> resultFuture = SafeFutureImpl.create();

                final FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless().add(new TransportFilter());
                if (protocol.isSecure) {
                    clientFilterChainBuilder.add(new SSLFilter(serverSSLEngineConfigurator, clientSSLEngineConfigurator));
                }

                clientFilterChainBuilder.add(new StringFilter(CHARSET)).add(new ClientResultFilter(protocol, resultFuture)).build();

                final SocketConnectorHandler connectorHandler = TCPNIOConnectorHandler.builder(transport).processor(clientFilterChainBuilder.build()).build();

                Future<Connection> future = connectorHandler.connect("localhost", PORT);
                connection = future.get();
                assertTrue(connection != null);

                connection.write(protocol.name);

                assertTrue(resultFuture.get(10, TimeUnit.SECONDS));
            }

        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    private PUProtocol createProtocol(final PUFilter puFilter, final ProtocolDescription protocolDescription) {

        final FilterChain chain = puFilter.getPUFilterChainBuilder().add(new StringFilter(CHARSET)).add(new SimpleResponseFilter(protocolDescription)).build();

        return new PUProtocol(new SimpleProtocolFinder(protocolDescription), chain);
    }

    private static String makeResponseMessage(ProtocolDescription protocolDescription) {
        return "Protocol-" + protocolDescription.name + (protocolDescription.isSecure ? "-secure" : "-plain");
    }

    private SSLContextConfigurator createSSLContextConfigurator() {
        SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
        ClassLoader cl = getClass().getClassLoader();
        // override system properties
        URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
        if (cacertsUrl != null) {
            sslContextConfigurator.setTrustStoreFile(cacertsUrl.getFile());
            sslContextConfigurator.setTrustStorePass("changeit");
        }

        // override system properties
        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        if (keystoreUrl != null) {
            sslContextConfigurator.setKeyStoreFile(keystoreUrl.getFile());
            sslContextConfigurator.setKeyStorePass("changeit");
        }

        return sslContextConfigurator;
    }

    private static final class SimpleProtocolFinder implements ProtocolFinder {
        private static final StringDecoder STRING_DECODER = new StringDecoder(CHARSET);

        public final ProtocolDescription protocolDescription;

        public SimpleProtocolFinder(final ProtocolDescription protocolDescription) {
            this.protocolDescription = protocolDescription;
        }

        @Override
        public Result find(PUContext puContext, FilterChainContext ctx) {
            final Buffer requestedProtocol = ctx.getMessage();
            final int bufferStart = requestedProtocol.position();

            final TransformationResult<Buffer, String> result = STRING_DECODER.transform(ctx.getConnection(), requestedProtocol);

            switch (result.getStatus()) {
            case COMPLETE:
                STRING_DECODER.release(ctx.getConnection());
                requestedProtocol.position(bufferStart);
                return protocolDescription.name.equals(result.getMessage()) ? Result.FOUND : Result.NOT_FOUND;
            case INCOMPLETE:
                return Result.NEED_MORE_DATA;

            default:
                STRING_DECODER.release(ctx.getConnection());
                requestedProtocol.position(bufferStart);
                return Result.NOT_FOUND;
            }
        }
    }

    private static final class SimpleResponseFilter extends BaseFilter {
        private final ProtocolDescription protocolDescription;

        public SimpleResponseFilter(ProtocolDescription protocolDescription) {
            this.protocolDescription = protocolDescription;
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            ctx.write(makeResponseMessage(protocolDescription));

            return ctx.getStopAction();
        }

    }

    private static final class ClientResultFilter extends BaseFilter {
        private final ProtocolDescription protocolDescription;
        private final String expectedResponse;
        private final FutureImpl<Boolean> resultFuture;

        public ClientResultFilter(ProtocolDescription protocolDescription, FutureImpl<Boolean> future) {
            this.protocolDescription = protocolDescription;
            this.resultFuture = future;
            expectedResponse = makeResponseMessage(protocolDescription);
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final String response = ctx.getMessage();
            if (expectedResponse.equals(response)) {
                resultFuture.result(Boolean.TRUE);
            } else {
                resultFuture.failure(new IllegalStateException("Unexpected response. Expect=" + expectedResponse + " come=" + response));
            }

            return ctx.getStopAction();
        }
    }

    private static final class ProtocolDescription {
        final String name;
        final boolean isSecure;

        public ProtocolDescription(String name, boolean isSecure) {
            this.name = name;
            this.isSecure = isSecure;
        }
    }

}
