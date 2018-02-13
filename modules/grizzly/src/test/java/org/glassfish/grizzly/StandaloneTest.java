/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.nio.transport.TCPNIOServerConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.streams.StreamReader;
import org.glassfish.grizzly.streams.StreamWriter;

/**
 * Test standalone Grizzly implementation.
 * 
 * @author Alexey Stashok
 */
public class StandaloneTest extends GrizzlyTestCase {
    private static final Logger logger = Grizzly.logger(StandaloneTest.class);
    
    public static final int PORT = 7780;
    
    public void testStandalone() throws Exception {
        TCPNIOTransport transport =
                TCPNIOTransportBuilder.newInstance().build();
        transport.getAsyncQueueIO().getWriter().setMaxPendingBytesPerConnection(-1);
        
        int messageSize = 166434;

        Connection connection = null;
        StreamReader reader;
        StreamWriter writer;

        try {
            // Enable standalone mode
            transport.configureStandalone(true);

            // Start listen on specific port
            final TCPNIOServerConnection serverConnection = transport.bind(PORT);
            // Start transport
            transport.start();

            // Start echo server thread
            final Thread serverThread =
                    startEchoServerThread(transport, serverConnection, messageSize);

            // Connect to the server
            Future<Connection> connectFuture = transport.connect("localhost", PORT);
            connection = connectFuture.get(10, TimeUnit.SECONDS);
            assertTrue(connectFuture.isDone());
            
            // fill out buffer
            byte[] buffer = new byte[messageSize];
            for(int i=0; i<messageSize; i++) {
                buffer[i] = (byte) (i % 128);
            }
            // write buffer
            writer = StandaloneProcessor.INSTANCE.getStreamWriter(connection);
            writer.writeByteArray(buffer);
            writer.flush();

            reader = StandaloneProcessor.INSTANCE.getStreamReader(connection);
            
            // prepare receiving buffer
            byte[] receiveBuffer = new byte[messageSize];


            Future readFuture = reader.notifyAvailable(messageSize);
            readFuture.get(20, TimeUnit.SECONDS);

            // Read the response.
            reader.readByteArray(receiveBuffer);

            assertTrue(readFuture.isDone());
            
            // Check the echo result
            assertTrue(Arrays.equals(buffer, receiveBuffer));

            serverThread.join(10 * 1000);
        } finally {
            if (connection != null) {
                connection.closeSilently();
            }

            transport.shutdownNow();
        }

    }

    private Thread startEchoServerThread(final TCPNIOTransport transport,
            final TCPNIOServerConnection serverConnection,
            final int messageSize) {
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Future<Connection> acceptFuture = serverConnection.accept();
                    Connection connection = acceptFuture.get(10, TimeUnit.SECONDS);
                    assertTrue(acceptFuture.isDone());

                    StreamReader reader =
                            StandaloneProcessor.INSTANCE.getStreamReader(connection);
                    StreamWriter writer =
                            StandaloneProcessor.INSTANCE.getStreamWriter(connection);
                    try {

                        Future readFuture = reader.notifyAvailable(messageSize);
                        readFuture.get(10, TimeUnit.SECONDS);
                        // Read until whole buffer will be filled out

                        byte[] buffer = new byte[messageSize];
                        reader.readByteArray(buffer);

                        assertTrue(readFuture.isDone());

                        // Write the echo
                        writer.writeByteArray(buffer);
                        Future writeFuture = writer.flush();
                        writeFuture.get(10, TimeUnit.SECONDS);

                        assertTrue(writeFuture.isDone());
                    } catch (Throwable e) {
                        logger.log(Level.WARNING,
                                "Error working with accepted connection", e);
                        assertTrue("Error working with accepted connection", false);
                    } finally {
                        connection.closeSilently();
                    }

                } catch (Exception e) {
                    if (!transport.isStopped()) {
                        logger.log(Level.WARNING,
                                "Error accepting connection", e);
                        assertTrue("Error accepting connection", false);
                    }
                }
            }
        });
        thread.start();
        
        return thread;
    }
}
