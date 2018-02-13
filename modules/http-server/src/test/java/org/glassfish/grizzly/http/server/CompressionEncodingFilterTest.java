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

package org.glassfish.grizzly.http.server;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.CompressionConfig.CompressionMode;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.Charsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class CompressionEncodingFilterTest {
    public enum HeaderType {String, Buffer, Chars}

    private final HeaderType headerType;
    private final Random r = new Random();
    
    public CompressionEncodingFilterTest(HeaderType headerType) {
        this.headerType = headerType;
    }

    @Parameters
    public static Collection<Object[]> getHeaderTypes() {
        return Arrays.asList(new Object[][]{
                    {HeaderType.String},
                    {HeaderType.Buffer},
                    {HeaderType.Chars}
                });
    }
    
    // ------------------------------------------------------------ Test Methods
    
    @Test
    public void testAcceptEncodingProcessing() throws Exception {

        final CompressionEncodingFilter filter =
                new CompressionEncodingFilter(CompressionMode.ON,
                                              1,
                                              new String[0],
                                              new String[0],
                                              new String[] {"gzip"});
        HttpRequestPacket request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "gzip");
        HttpResponsePacket response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).build();
        assertTrue(filter.applyEncoding(response));
        
        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "foo, gzip;q=1.0, foo2");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).build();
        assertTrue(filter.applyEncoding(response));
        
        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "foo, gzip; q=1.0, foo2");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).build();
        assertTrue(filter.applyEncoding(response));

        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "foo, gzip;q=0, foo2");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).build();
        assertFalse(filter.applyEncoding(response));

        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "foo, gzip; q=0, foo2");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).build();
        assertFalse(filter.applyEncoding(response));

        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "compress; q=0.5, gzip;q=1.0");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).build();
        assertTrue(filter.applyEncoding(response));

        // Check double-compression
        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "foo, gzip;q=1.0, foo2");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).header(Header.ContentEncoding, "gzip").build();
        assertFalse(filter.applyEncoding(response));
    }

    @Test
    public void testMinSizeSetting() throws Exception {

        final CompressionEncodingFilter filter =
                new CompressionEncodingFilter(CompressionMode.ON,
                                              1024,
                                              new String[0],
                                              new String[0],
                                              new String[] {"gzip"});
        HttpRequestPacket request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "compress;q=0.5, gzip;q=1.0");
        HttpResponsePacket response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).contentLength(1023).build();
        assertFalse(filter.applyEncoding(response));
        
        request = setAcceptEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "compress;q=0.5, gzip;q=1.0");
        response = HttpResponsePacket.builder(request).protocol(Protocol.HTTP_1_1).contentLength(1024).build();
        assertTrue(filter.applyEncoding(response));
    }

    @Test
    public void testContentEncodingProcessing() throws Exception {
        
        final CompressionEncodingFilter filter =
                new CompressionEncodingFilter(CompressionMode.ON,
                                              1,
                                              new String[0],
                                              new String[0],
                                              new String[] {"gzip"},
                                              true);
        
        // Valid gzip compression
        HttpRequestPacket request = setContentEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "gzip");
        assertTrue(filter.applyDecoding(request));
        
        // Other encoding header
        request = setContentEncoding(
                HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build(),
                "identity");
        assertFalse(filter.applyDecoding(request));
        
        // No header - assume uncompressed
        request = HttpRequestPacket.builder().method(Method.GET).protocol(Protocol.HTTP_1_1).uri("/").build();
        assertFalse(filter.applyDecoding(request));
    }
    
    private HttpRequestPacket setAcceptEncoding(HttpRequestPacket request, String acceptEncoding) {
        return setHeader(request, Header.AcceptEncoding, acceptEncoding);
    }

    private HttpRequestPacket setContentEncoding(HttpRequestPacket request, String contentEncoding) {
        return setHeader(request, Header.ContentEncoding, contentEncoding);
    }

    private HttpRequestPacket setHeader(HttpRequestPacket request, Header header, String headerValue) {
        switch (headerType) {
            case String: {
                request.addHeader(header, headerValue);
                break;
            }
            case Buffer: {
                final byte[] encodingBytes =
                        headerValue.getBytes(Charsets.ASCII_CHARSET);
                
                final byte[] array = new byte[2048];
                final int offs = r.nextInt(array.length - encodingBytes.length);
                System.arraycopy(encodingBytes, 0, array, offs, encodingBytes.length);
                final Buffer b = Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, array);
                
                request.getHeaders().addValue(header)
                        .setBuffer(b, offs, offs + encodingBytes.length);
                break;
            }
                
            case Chars: {
                final char[] array = new char[2048];
                final int offs = r.nextInt(array.length - headerValue.length());
                
                headerValue.getChars(0, headerValue.length(), array, offs);
                
                request.getHeaders().addValue(header)
                        .setChars(array, offs, offs + headerValue.length());
                break;
            }
        }
        
        return request;
    }
}
