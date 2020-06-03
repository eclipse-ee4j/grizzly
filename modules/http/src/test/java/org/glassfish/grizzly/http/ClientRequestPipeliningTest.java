/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;

import junit.framework.TestCase;

public class ClientRequestPipeliningTest extends TestCase {

    private static final int PORT = 9933;

    // ------------------------------------------------------------ Test Methods

    /*
     * This test merely ensures that it's possible to rapidly fire requests without waiting for a response. The code will
     * now poll() a queue to associate a request with a response. This *does not* however mean that we do any validation of
     * the connect (i.e., keep-alive), error handling or other pipeline semantics (yet).
     */
    @SuppressWarnings({ "unchecked" })
    public void testPipelinedRequests() throws Exception {

        final int requestCount = 5;
        final CountDownLatch latch = new CountDownLatch(requestCount);
        final ResponseCollectingFilter responseFilter = new ResponseCollectingFilter(latch);
        final TCPNIOTransport clientTransport = createClientTransport(responseFilter);
        final TCPNIOTransport serverTransport = createServerTransport();
        try {
            serverTransport.bind(PORT);
            serverTransport.start();
            clientTransport.start();

            final GrizzlyFuture<Connection> connFuture = clientTransport.connect("localhost", PORT);

            final Connection c = connFuture.get(15, TimeUnit.SECONDS);
            for (int i = 0; i < requestCount; i++) {
                final HttpRequestPacket.Builder reqBuilder = HttpRequestPacket.builder();
                reqBuilder.method(Method.GET);
                reqBuilder.uri("/");
                reqBuilder.protocol(Protocol.HTTP_1_1);
                reqBuilder.header(Header.Host, "localhost:" + PORT);
                reqBuilder.contentLength(0);
                c.write(reqBuilder.build());
            }
            latch.await(30, TimeUnit.SECONDS);

            assertEquals(requestCount, responseFilter.responses.size());
            for (int i = 0; i < requestCount; i++) {
                assertEquals(i + 1, responseFilter.responses.get(i).intValue());
            }
        } finally {
            clientTransport.shutdownNow();
            serverTransport.shutdownNow();
        }
    }

    // --------------------------------------------------------- Private Methods

    private TCPNIOTransport createClientTransport(final Filter clientFilter) {

        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        final FilterChainBuilder b = FilterChainBuilder.stateless();
        b.add(new TransportFilter());
        b.add(new HttpClientFilter());
        b.add(clientFilter);
        transport.setProcessor(b.build());
        return transport;

    }

    private TCPNIOTransport createServerTransport() {

        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();
        final FilterChainBuilder b = FilterChainBuilder.stateless();
        b.add(new TransportFilter());
        b.add(new HttpServerFilter());
        b.add(new SimpleResponseFilter());
        transport.setProcessor(b.build());
        return transport;
    }

    // ---------------------------------------------------------- Nested Classes

    private static final class ResponseCollectingFilter extends BaseFilter {

        private final CountDownLatch latch;
        final List<Integer> responses = new ArrayList<>();

        ResponseCollectingFilter(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {

            final Object message = ctx.getMessage();
            if (message instanceof HttpContent) {
                final HttpContent content = (HttpContent) message;
                if (content.getContent().hasRemaining()) {
                    final String result = content.getContent().toStringContent();
                    responses.add(Integer.parseInt(result));
                    latch.countDown();
                }
            }
            return ctx.getStopAction();
        }

    } // END ResponseCollectingFilter

    private static final class SimpleResponseFilter extends BaseFilter {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {

            final int count = counter.incrementAndGet();
            HttpRequestPacket request = (HttpRequestPacket) ((HttpContent) ctx.getMessage()).getHttpHeader();
            HttpResponsePacket response = request.getResponse();
            response.setStatus(HttpStatus.OK_200);
            final HttpContent content = response.httpContentBuilder().content(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, Integer.toString(count)))
                    .build();
            content.setLast(true);
            ctx.write(content);
            return ctx.getStopAction();

        }

    } // END SimpleResponseFilter

}
