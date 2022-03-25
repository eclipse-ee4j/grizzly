/*
 * Copyright (c) 2022 Eclipse Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.filterchain;

import java.nio.channels.ServerSocketChannel;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext.Operation;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author David Matejcek
 */
public class FilterChainContextTest {

    @Test
    public void testToString_null() throws Exception {
        assertEquals("FilterChainContext [connection=null, closeable=null, operation=NONE, message=null, address=null]",
            new FilterChainContext().toString());
    }

    @Test
    public void testToString_usual() throws Exception {
        try (ServerSocketChannel channel = ServerSocketChannel.open()) {
            final TCPNIOConnection connection = new TCPNIOConnection(new TCPNIOTransport(), channel);
            final FilterChainContext context = FilterChainContext.create(connection);
            final Buffer buffer = Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, "Ororok orebuh");
            context.setAddress("localhost");
            context.setMessage(buffer);
            context.setOperation(Operation.CONNECT);
            assertEquals("FilterChainContext ["
                + "connection=TCPNIOConnection{localSocketAddress=null, peerSocketAddress=null}, "
                + "closeable=TCPNIOConnection{localSocketAddress=null, peerSocketAddress=null}, "
                + "operation=CONNECT, "
                + "message=" + buffer + ", " // contains hashcode
                + "address=localhost"
                + "]",
                context.toString());
        }
    }
}
