/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.servlet.extras.util;

import java.nio.charset.Charset;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.server.Constants;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;

import java.util.ArrayList;
import java.util.List;

import static org.glassfish.grizzly.http.util.HttpCodecUtils.put;
/**
 * HTTP Multi-part packet builder
 * 
 * @author Alexey Stashok
 */
public class MultipartPacketBuilder {
    private static final Charset DEFAULT_HTTP_CHARSET = org.glassfish.grizzly.http.util.Constants.DEFAULT_HTTP_CHARSET;
    private static final byte[] DOUBLE_DASH = "--".getBytes(DEFAULT_HTTP_CHARSET);
    private static final byte[] CONTENT_DISPOSITION_BYTES = "Content-Disposition".getBytes(DEFAULT_HTTP_CHARSET);
    private static final byte[] CONTENT_TYPE_BYTES = "Content-Type".getBytes(DEFAULT_HTTP_CHARSET);
    private final byte[] tempEncodingBuffer = new byte[512];
    
    private final String boundary;

    private final List<MultipartEntryPacket> multipartEntries =
            new ArrayList<MultipartEntryPacket>();

    private String epilogue;
    private String preamble;

    private MultipartPacketBuilder(final String boundary) {
        this.boundary = boundary;
    }

    public static MultipartPacketBuilder builder(final String boundary) {
        return new MultipartPacketBuilder(boundary);
    }

    public MultipartPacketBuilder addMultipartEntry(final MultipartEntryPacket multipartEntry) {
        multipartEntries.add(multipartEntry);
        return this;
    }

    public MultipartPacketBuilder removeMultipartEntry(final MultipartEntryPacket multipartEntry) {
        multipartEntries.remove(multipartEntry);
        return this;
    }

    public MultipartPacketBuilder preamble(String preamble) {
        this.preamble = preamble;
        return this;
    }

    public MultipartPacketBuilder epilogue(String epilogue) {
        this.epilogue = epilogue;
        return this;
    }

    public Buffer build() {
        final MemoryManager memoryManager = MemoryManager.DEFAULT_MEMORY_MANAGER;
        Buffer resultBuffer = null;

        boolean isFirst = true;
        for (MultipartEntryPacket entry : multipartEntries) {
            
            Buffer headerBuffer = memoryManager.allocate(2048);

            if (!isFirst) {
                headerBuffer = put(memoryManager, headerBuffer, Constants.CRLF_BYTES);
            } else {
                if (preamble != null) {
                    headerBuffer = put(memoryManager, headerBuffer, tempEncodingBuffer, preamble);
                    headerBuffer = put(memoryManager, headerBuffer, Constants.CRLF_BYTES);
                }
                
                isFirst = false;
            }

            headerBuffer = put(memoryManager, headerBuffer, DOUBLE_DASH);
            headerBuffer = put(memoryManager, headerBuffer, tempEncodingBuffer, boundary);
            headerBuffer = put(memoryManager, headerBuffer, Constants.CRLF_BYTES);
            
            for (String headerName : entry.getHeaderNames()) {
                String headerValue = entry.getHeader(headerName);
                setHeader(memoryManager, headerBuffer, tempEncodingBuffer, headerName, headerValue);
            }

            if (entry.getContentDisposition() != null) {
                setHeader(memoryManager, headerBuffer, tempEncodingBuffer,
                        CONTENT_DISPOSITION_BYTES, entry.getContentDisposition());
            }

            if (entry.getContentType() != null) {
                setHeader(memoryManager, headerBuffer, tempEncodingBuffer,
                        CONTENT_TYPE_BYTES, entry.getContentType());
            }

            headerBuffer = put(memoryManager, headerBuffer, Constants.CRLF_BYTES);
            headerBuffer.trim();
            resultBuffer = Buffers.appendBuffers(memoryManager, resultBuffer,
                    headerBuffer);
            resultBuffer = Buffers.appendBuffers(memoryManager, resultBuffer,
                    entry.getContent());

            isFirst = false;
        }
        
        Buffer trailerBuffer = memoryManager.allocate(boundary.length() + 8);
        trailerBuffer = put(memoryManager, trailerBuffer, Constants.CRLF_BYTES);
        trailerBuffer = put(memoryManager, trailerBuffer, DOUBLE_DASH);
        trailerBuffer = put(memoryManager, trailerBuffer, tempEncodingBuffer, boundary);
        trailerBuffer = put(memoryManager, trailerBuffer, DOUBLE_DASH);
        trailerBuffer = put(memoryManager, trailerBuffer, Constants.CRLF_BYTES);

        if (epilogue != null) {
            trailerBuffer = put(memoryManager, trailerBuffer, tempEncodingBuffer, epilogue);
        }

        trailerBuffer.flip();

        resultBuffer = Buffers.appendBuffers(memoryManager, resultBuffer, trailerBuffer);
        
        return resultBuffer;
    }

    private static void setHeader(final MemoryManager memoryManager,
                                  Buffer headerBuffer,
                                  byte[] tempEncodingBuffer,
                                  String headerName,
                                  String headerValue) {
        
        headerBuffer = put(memoryManager, headerBuffer, tempEncodingBuffer, headerName);
        headerBuffer = put(memoryManager, headerBuffer, Constants.COLON_BYTES);
        headerBuffer = put(memoryManager, headerBuffer, null, headerValue);
        put(memoryManager, headerBuffer, Constants.CRLF_BYTES);
    }

    private static void setHeader(final MemoryManager memoryManager,
                                  Buffer headerBuffer,
                                  byte[] tempEncodingBuffer,
                                  byte[] headerName,
                                  String headerValue) {

        headerBuffer = put(memoryManager, headerBuffer, headerName);
        headerBuffer = put(memoryManager, headerBuffer, Constants.COLON_BYTES);
        headerBuffer = put(memoryManager, headerBuffer, tempEncodingBuffer, headerValue);
        put(memoryManager, headerBuffer, Constants.CRLF_BYTES);
    }

}
