/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Iterator;
import junit.framework.TestCase;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.MimeHeaders;

public class HttpResponsePacketTest extends TestCase {
    
    private HttpResponsePacket response;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        response = HttpResponsePacket.builder(
                HttpRequestPacket.builder().uri("/").protocol(Protocol.HTTP_1_1).build())
                .build();
    }

    public void testSpecialHeadersTest() throws Exception {
        
        assertFalse(response.containsHeader("Content-Length"));
        assertFalse(response.containsHeader("Content-type"));
        assertFalse(response.containsHeader(Header.ContentLength));
        assertFalse(response.containsHeader(Header.ContentType));
        
        assertNull(response.getHeader("Content-Length"));
        assertNull(response.getHeader("Content-type"));
        assertNull(response.getHeader(Header.ContentLength));
        assertNull(response.getHeader(Header.ContentType));
        
        response.setHeader("Content-Length", "1");
        assertEquals(1L, response.getContentLength());
        assertEquals("1", response.getHeader("Content-length"));
        assertTrue(response.containsHeader("content-length"));
        response.setHeader(Header.ContentLength, "2");
        assertEquals(2L, response.getContentLength());
        assertEquals("2", response.getHeader(Header.ContentLength));
        assertTrue(response.containsHeader(Header.ContentLength));
        
        response.addHeader("content-Length", "3");
        assertEquals(3L, response.getContentLength());
        assertEquals("3", response.getHeader("Content-length"));
        response.addHeader(Header.ContentLength, "4");
        assertEquals(4L, response.getContentLength());
        assertEquals("4", response.getHeader(Header.ContentLength));

        response.setHeader("Content-Type", "text/plain");
        assertEquals("text/plain", response.getContentType());
        assertEquals("text/plain", response.getHeader("Content-type"));
        assertTrue(response.containsHeader("content-Type"));
        response.setHeader(Header.ContentType, "text/xml");
        assertEquals("text/xml", response.getContentType());
        assertEquals("text/xml", response.getHeader(Header.ContentType));
        assertTrue(response.containsHeader(Header.ContentType));

        response.addHeader("content-Type", "text/plain");
        assertEquals("text/plain", response.getContentType());
        assertEquals("text/plain", response.getHeader("Content-type"));
        response.addHeader(Header.ContentType, "text/xml");
        assertEquals("text/xml", response.getContentType());
        assertEquals("text/xml", response.getHeader(Header.ContentType));
    }

    /**
     * http://java.net/jira/browse/GRIZZLY-1295
     * "NullPointer while trying to get next value via ValuesIterator in MimeHeaders"
     */
    public void testMimeHeaderIterators() {
        response.setHeader("Content-Length", "1");
        response.setHeader("Content-Type", "text/plain");
        response.setHeader("Host", "localhost");
        
        // Headers iterator test
        boolean removed = false;
        
        final MimeHeaders headers = response.getHeaders();
        for (Iterator<String> it = headers.names().iterator(); it.hasNext();) {
            it.next();
            
            if (!removed) {
                it.remove();
                removed = true;
            }
        }
        
        removed = false;
        
        final String multiValueHeader = "Multi-Value";
        
        response.addHeader(multiValueHeader, "value-1");
        response.addHeader(multiValueHeader, "value-2");
        response.addHeader(multiValueHeader, "value-3");
        
        for (Iterator<String> it = headers.values(multiValueHeader).iterator(); it.hasNext();) {
            it.next();
            
            if (!removed) {
                it.remove();
                removed = true;
            }
        }
    }

    public void testToString() {
        response = HttpResponsePacket.builder(
                HttpRequestPacket.builder()
                        .uri("/")
                        .protocol(Protocol.HTTP_1_1)
                        .build())
                .header("transfer-encoding", "chunked")
                .header("some-header", "firstValue")
                .header("some-header", "secondValue")
                .build();

        assertEquals(response.toString(), "HttpResponsePacket (\n"
                + "  status=200\n"
                + "  reason=OK\n"
                + "  protocol=HTTP/0.9\n"
                + "  content-length=-1\n"
                + "  committed=false\n"
                + "  headers=[\n"
                + "      transfer-encoding=chunked\n"
                + "      some-header=firstValue\n"
                + "      some-header=secondValue]\n"
                + ")");
    }
    
}
