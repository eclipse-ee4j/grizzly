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

package org.glassfish.grizzly.http2;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.Connection;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.Charsets;
import org.junit.Test;

import static org.junit.Assert.*;
/**
 * {@link NetworkListener} tests.
 * 
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class NetworkListenerTest extends AbstractHttp2Test {
    public static final int PORT = 18897;

    @Test
    public void testGracefulShutdown() throws Exception {
        final String msg = "Hello World";
        final byte[] msgBytes = msg.getBytes(Charsets.UTF8_CHARSET);
        
        final BlockingQueue<HttpContent> clientInQueue =
                new LinkedBlockingQueue<>();
        
        final FilterChain filterChain =
                createClientFilterChainAsBuilder(false,
                new BaseFilter() {
            @Override
            public NextAction handleRead(FilterChainContext ctx) throws IOException {
                final HttpContent httpContent = ctx.getMessage();
                clientInQueue.add(httpContent);

                return ctx.getStopAction();
            }
        }).build();
        
        final TCPNIOTransport clientTransport =
                TCPNIOTransportBuilder.newInstance()
                .setProcessor(filterChain)
                .build();

        final HttpServer server = createServer(null, PORT, false,
                HttpHandlerRegistration.of(new HttpHandler() {
                    @Override
                    public void service(Request request, Response response) throws Exception {
                        response.setContentType("text/plain");
                        response.setCharacterEncoding(Charsets.UTF8_CHARSET.name());
                        response.setContentLength(msgBytes.length);
                        response.flush();
                        Thread.sleep(2000);
                        response.getOutputStream().write(msgBytes);
                    }
                }, "/path"));

        try {
            server.start();
            clientTransport.start();

            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            Connection connection = null;
            try {
                connection = connectFuture.get(10, TimeUnit.SECONDS);
                final HttpRequestPacket requestPacket =
                        (HttpRequestPacket) createRequest(PORT, "GET",
                        null, null);
                connection.write(requestPacket);
                
                HttpContent response = clientInQueue.poll(10,
                        TimeUnit.SECONDS);
                assertNotNull("Response can't be null", response);
                
                final HttpResponsePacket responseHeader =
                        (HttpResponsePacket) response.getHttpHeader();
                
                assertEquals(200, responseHeader.getStatus());
                assertEquals(msgBytes.length, responseHeader.getContentLength());
                
                final Future<HttpServer> gracefulFuture = server.shutdown();

                while (!response.isLast()) {
                    final HttpContent chunk = clientInQueue.poll(10, TimeUnit.SECONDS);
                    assertNotNull("Chunk can't be null", chunk);
                    
                    response = response.append(chunk);
                }

                final String content = response.getContent().toStringContent(
                        Charsets.UTF8_CHARSET);
                assertEquals(msg, content);
                
                assertNotNull(gracefulFuture.get(5, TimeUnit.SECONDS));
            } finally {
                // Close the client connection
                if (connection != null) {
                    connection.closeSilently();
                }
            }            
        } finally {
            clientTransport.shutdownNow();
            server.shutdownNow();
        }
    }    
}
