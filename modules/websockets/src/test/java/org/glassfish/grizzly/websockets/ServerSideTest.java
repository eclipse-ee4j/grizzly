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

package org.glassfish.grizzly.websockets;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SuppressWarnings({ "StringContatenationInLoop" })
@RunWith(Parameterized.class)
public class ServerSideTest extends BaseWebSocketTestUtilities {
    public static final int ITERATIONS = 5000;
    private final Version version;

    public ServerSideTest(Version version) {
        this.version = version;
    }

    @Test
    public void steadyFlow() throws IOException, InstantiationException, ExecutionException, InterruptedException, URISyntaxException {
        WebSocketServer server = WebSocketServer.createServer(PORT);
        server.register("", "/echo", new EchoApplication());
        server.start();
        TrackingWebSocket socket = new TrackingWebSocket(String.format("ws://localhost:%s/echo", PORT), version, 5 * ITERATIONS);
        socket.connect();
        try {
            int count = 0;
            final Date start = new Date();
            final int marker = ITERATIONS / 5;
            while (count++ < ITERATIONS) {
                /*
                 * if (count % marker == 0) { System.out.printf("Running iteration %s of %s\n", count, ITERATIONS); }
                 */
                socket.send("test message: " + count);
                socket.send("let's try again: " + count);
                socket.send("3rd time's the charm!: " + count);
                socket.send("ok.  just one more: " + count);
                socket.send("now, we're done: " + count);
            }
            Assert.assertTrue("All messages should come back: " + socket.getReceived(), socket.waitOnMessages());
            time("ServerSideTest.steadyFlow (" + version + ")", start, new Date());
        } finally {
            if (socket != null) {
                socket.close();
            }
            server.stop();
        }
    }

//    @Test
    public void single() throws IOException, InstantiationException, ExecutionException, InterruptedException, URISyntaxException {
        WebSocketServer server = WebSocketServer.createServer(PORT);
        server.register("", "/echo", new EchoApplication());
        server.start();
        TrackingWebSocket socket = new TrackingWebSocket(String.format("ws://localhost:%s/echo", PORT), version, 1);
        socket.connect();
        try {
            int count = 0;
            final Date start = new Date();
            socket.send("test message: " + count);
            Assert.assertTrue("All messages should come back: " + socket.getReceived(), socket.waitOnMessages());
        } finally {
            if (socket != null) {
                socket.close();
            }
            server.stop();
        }
    }

    @Test
    @SuppressWarnings({ "StringContatenationInLoop" })
    public void sendAndWait() throws IOException, InstantiationException, InterruptedException, ExecutionException, URISyntaxException {
        WebSocketServer server = WebSocketServer.createServer(PORT);
        server.register("", "/echo", new EchoApplication());
        server.start();
        CountDownWebSocket socket = new CountDownWebSocket(String.format("ws://localhost:%s/echo", PORT), version);
        socket.connect();
        try {
            int count = 0;
            final Date start = new Date();
            while (count++ < ITERATIONS) {
                /*
                 * if (count % ITERATIONS / 5 == 0) { System.out.printf("Running iteration %s of %s\n", count, ITERATIONS); }
                 */
                socket.send("test message " + count);
                socket.send("let's try again: " + count);
                socket.send("3rd time's the charm!: " + count);
                Assert.assertTrue("Everything should come back", socket.countDown());
                socket.send("ok.  just one more: " + count);
                socket.send("now, we're done: " + count);
                Assert.assertTrue("Everything should come back", socket.countDown());
            }
            time("ServerSideTest.sendAndWait (" + version + ")", start, new Date());
        } finally {
            if (socket != null) {
                socket.close();
            }
            server.stop();
        }
    }

//    @Test
    public void multipleClients() throws IOException, InstantiationException, ExecutionException, InterruptedException, URISyntaxException {
        WebSocketServer server = WebSocketServer.createServer(PORT);
        server.register("", "/echo", new EchoApplication());
        server.start();
        List<TrackingWebSocket> clients = new ArrayList<>();
        try {
            final String address = String.format("ws://localhost:%s/echo", PORT);
            for (int x = 0; x < 5; x++) {
                final TrackingWebSocket socket = new TrackingWebSocket(address, x + "", version, 5 * ITERATIONS);
                socket.connect();
                clients.add(socket);
            }
            String[] messages = { "test message", "let's try again", "3rd time's the charm!", "ok.  just one more", "now, we're done" };
            for (int count = 0; count < ITERATIONS; count++) {
                for (String message : messages) {
                    for (TrackingWebSocket socket : clients) {
                        socket.send(String.format("%s: count %s: %s", socket.getName(), count, message));
                    }
                }
            }
            for (TrackingWebSocket socket : clients) {
                Assert.assertTrue("All messages should come back: " + socket.getReceived(), socket.waitOnMessages());
            }
        } finally {
            server.stop();
        }
    }

    @Test
    public void bigPayload() throws IOException, InstantiationException, ExecutionException, InterruptedException, URISyntaxException {
        WebSocketServer server = WebSocketServer.createServer(PORT);
        server.register("", "/echo", new EchoApplication());
        server.start();
        final int count = 5;
        final CountDownLatch received = new CountDownLatch(count);
        WebSocketClient socket = new WebSocketClient(String.format("ws://localhost:%s/echo", PORT)) {
            @Override
            public void onMessage(String frame) {
                received.countDown();
            }
        };
        socket.connect();
        try {
            StringBuilder sb = new StringBuilder();
            while (sb.length() < 10000) {
                sb.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus quis lectus odio, et"
                        + " dictum purus. Suspendisse id ante ac tortor facilisis porta. Nullam aliquet dapibus dui, ut"
                        + " scelerisque diam luctus sit amet. Donec faucibus aliquet massa, eget iaculis velit ullamcorper"
                        + " eu. Fusce quis condimentum magna. Vivamus eu feugiat mi. Cras varius convallis gravida. Vivamus"
                        + " et elit lectus. Aliquam egestas, erat sed dapibus dictum, sem ligula suscipit mauris, a"
                        + " consectetur massa augue vel est. Nam bibendum varius lobortis. In tincidunt, sapien quis"
                        + " hendrerit vestibulum, lorem turpis faucibus enim, non rhoncus nisi diam non neque. Aliquam eu"
                        + " urna urna, molestie aliquam sapien. Nullam volutpat, erat condimentum interdum viverra, tortor"
                        + " lacus venenatis neque, vitae mattis sem felis pellentesque quam. Nullam sodales vestibulum"
                        + " ligula vitae porta. Aenean ultrices, ligula quis dapibus sodales, nulla risus sagittis sapien,"
                        + " id posuere turpis lectus ac sapien. Pellentesque sed ante nisi. Quisque eget posuere sapien.");
            }
            final String data = sb.toString();
            for (int x = 0; x < count; x++) {
                socket.send(data);
            }
            Assert.assertTrue("Message should come back", received.await(300, TimeUnit.SECONDS));
        } finally {
            if (socket != null) {
                socket.close();
            }
            server.stop();
        }

    }

    private void time(String method, Date start, Date end) {
        final int total = 5 * ITERATIONS;
        final double time = (end.getTime() - start.getTime()) / 1000.0;
        System.out.printf("%s: sent %s messages in %.3fs for %.3f msg/s\n", method, total, time, total / time);
    }

}
