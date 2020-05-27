/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.memory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyTestCase;
import org.glassfish.grizzly.StandaloneProcessor;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.streams.StreamReader;
import org.glassfish.grizzly.streams.StreamWriter;
import org.glassfish.grizzly.utils.Pair;

/**
 * Test how {@link CompositeBuffer} works with Streams.
 *
 * @author Alexey Stashok
 */
public class CompositeBufferInStreamTest extends GrizzlyTestCase {

    public static final int PORT = 7783;
    private static final Logger LOGGER = Grizzly.logger(CompositeBufferInStreamTest.class);

    @SuppressWarnings("unchecked")
    public void testCompositeBuffer() throws Exception {
        Connection connection = null;
        final TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance().build();

        final Buffer portion1 = Buffers.wrap(transport.getMemoryManager(), "Hello");
        final Buffer portion2 = Buffers.wrap(transport.getMemoryManager(), " ");
        final Buffer portion3 = Buffers.wrap(transport.getMemoryManager(), "world!");

        final FutureImpl<Integer> lock1 = SafeFutureImpl.create();
        final FutureImpl<Integer> lock2 = SafeFutureImpl.create();
        final FutureImpl<Integer> lock3 = SafeFutureImpl.create();

        final Pair<Buffer, FutureImpl<Integer>>[] portions = new Pair[] { new Pair<>(portion1, lock1),
                new Pair<>(portion2, lock2), new Pair<>(portion3, lock3) };

        try {
            // Start listen on specific port
            final TCPNIOServerConnection serverConnection = transport.bind(PORT);

            transport.configureStandalone(true);

            transport.start();

            // Start echo server thread
            startEchoServerThread(transport, serverConnection, portions);

            Future<Connection> future = transport.connect("localhost", PORT);
            connection = future.get(10, TimeUnit.SECONDS);
            assertTrue(connection != null);

            connection.configureStandalone(true);

            final StreamWriter writer = ((StandaloneProcessor) connection.getProcessor()).getStreamWriter(connection);

            for (Pair<Buffer, FutureImpl<Integer>> portion : portions) {
                final Buffer buffer = portion.getFirst().duplicate();
                final Future<Integer> locker = portion.getSecond();

                writer.writeBuffer(buffer);
                final Future<Integer> writeFuture = writer.flush();
                writeFuture.get(5000, TimeUnit.MILLISECONDS);

                locker.get(5000, TimeUnit.MILLISECONDS);
            }

            assertTrue(true);

        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }
    }

    private void startEchoServerThread(final TCPNIOTransport transport, final TCPNIOServerConnection serverConnection,
            final Pair<Buffer, FutureImpl<Integer>>[] portions) {
        new Thread(new Runnable() {

            @Override
            public void run() {
//                while (!transport.isStopped()) {
                try {
                    Future<Connection> acceptFuture = serverConnection.accept();
                    Connection connection = acceptFuture.get(10, TimeUnit.SECONDS);
                    assertTrue(acceptFuture.isDone());

                    int availableExp = 0;

                    StreamReader reader = ((StandaloneProcessor) connection.getProcessor()).getStreamReader(connection);

                    int i = 0;
                    try {
                        for (; i < portions.length; i++) {
                            final Pair<Buffer, FutureImpl<Integer>> portion = portions[i];
                            final FutureImpl<Integer> currentLocker = portion.getSecond();

                            availableExp += portion.getFirst().remaining();
                            Future readFuture = reader.notifyAvailable(availableExp);
                            readFuture.get(30, TimeUnit.SECONDS);

                            if (readFuture.isDone()) {
                                final Buffer compositeBuffer = reader.getBufferWindow();
                                int counter = 0;
                                for (int j = 0; j <= i; j++) {
                                    final Buffer currentBuffer = portions[j].getFirst();
                                    for (int k = 0; k < currentBuffer.limit(); k++) {
                                        final byte found = compositeBuffer.get(counter++);
                                        final byte expected = currentBuffer.get(k);
                                        if (found != expected) {
                                            currentLocker.failure(new IllegalStateException("CompositeBuffer content is broken. Offset: "
                                                    + compositeBuffer.position() + " found: " + found + " expected: " + expected));
                                            return;
                                        }
                                    }
                                }
                            } else {
                                currentLocker.failure(new IllegalStateException("Error reading content portion: " + i));
                                return;
                            }

                            currentLocker.result(i);
                        }
                        // Read until whole buffer will be filled out
                    } catch (Throwable e) {
                        portions[i].getSecond().failure(e);
                        LOGGER.log(Level.WARNING, "Error working with accepted connection on step: " + i, e);
                    } finally {
                        connection.closeSilently();
                    }

                } catch (Exception e) {
                    if (!transport.isStopped()) {
                        LOGGER.log(Level.WARNING, "Error accepting connection", e);
                        assertTrue("Error accepting connection", false);
                    }
                }
//                }
            }
        }).start();
    }
}
