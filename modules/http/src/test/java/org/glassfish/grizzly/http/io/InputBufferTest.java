/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.io;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.nio.transport.TCPNIOConnection;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Grizzly team
 */
public class InputBufferTest {
    /**
     * GRIZZLY-1742
     * @throws Throwable 
     */
    @Test
    public void testBinaryMarkResetOnSimpleToComposite() throws Throwable {
        TCPNIOTransport dummyTransport = TCPNIOTransportBuilder.newInstance().build();
        TCPNIOConnection dummyConnection = new TCPNIOConnection(dummyTransport, null);
        FilterChainContext dummyFcc = FilterChainContext.create(dummyConnection);
        
        final HttpHeader httpHeader = HttpRequestPacket.builder()
                .method(Method.POST)
                .uri("/")
                .protocol(Protocol.HTTP_1_1)
                .host("localhost:8080")
                .contentLength(7)
                .build();
        
        final HttpContent emptyContent = HttpContent.builder(httpHeader)
                .content(Buffers.EMPTY_BUFFER)
                .build();
        
        dummyFcc.setMessage(emptyContent);
        
        final InputBuffer ib = new InputBuffer();
        ib.initialize(httpHeader, dummyFcc);
        
        ib.append(emptyContent);
        
        ib.mark(1);
        
        Buffer payload = Buffers.wrap(
                MemoryManager.DEFAULT_MEMORY_MANAGER, "JunkJunkJunkPayload");
        payload.position(payload.limit() - "Payload".length()); // make 'Payload' visible
        
        final HttpContent payloadContent = HttpContent.builder(httpHeader)
                .content(payload)
                .build();
        ib.append(payloadContent);
        
        assertEquals('P', (char) ib.readByte()); // first payload byte
        
        ib.reset();
        
        assertEquals('P', (char) ib.readByte()); // first payload byte
    }
}
