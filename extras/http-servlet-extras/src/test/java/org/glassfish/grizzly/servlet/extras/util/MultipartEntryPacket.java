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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.utils.Charsets;
import org.glassfish.grizzly.http.util.ContentType;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.memory.Buffers;

import java.nio.charset.Charset;

/**
 * MultipartEntry packet.
 * 
 * @author Alexey Stashok
 */
public class MultipartEntryPacket {

    public static Builder builder() {
        return new Builder();
    }

    private String contentDisposition;
    private String contentType;
    protected final MimeHeaders headers = new MimeHeaders();

    private Buffer content;

    private MultipartEntryPacket() {
    }

    public String getContentDisposition() {
        return contentDisposition;
    }

    public String getContentType() {
        return contentType;
    }

    public Buffer getContent() {
        return content;
    }

    public Iterable<String> getHeaderNames() {
        return headers.names();
    }

    public String getHeader(String name) {
        return headers.getHeader(name);
    }

    // ---------------------------------------------------------- Nested Classes


    /**
     * <tt>HttpRequestPacket</tt> message builder.
     */
    public static class Builder {
        private final MultipartEntryPacket packet;
        
        protected Builder() {
            packet = new MultipartEntryPacket();
        }

        public Builder contentDisposition(String contentDisposition) {
            packet.contentDisposition = contentDisposition;
            return this;
        }

        public Builder contentType(String contentType) {
            packet.contentType = contentType;
            return this;
        }

        public Builder header(String name, String value) {
            if ("content-disposition".equalsIgnoreCase(name)) {
                contentDisposition(value);
            } else if ("content-type".equalsIgnoreCase(name)) {
                contentType(value);
            } else {
                packet.headers.addValue(name).setString(value);
            }
            return this;
        }

        public Builder content(Buffer content) {
            packet.content = content;
            return this;
        }

        public Builder content(String content) {
            return content(content, getCharset());
        }

        public Builder content(String content, Charset charset) {
            packet.content = Buffers.wrap(null, content, charset);
            return this;
        }

        /**
         * Build the <tt>HttpRequestPacket</tt> message.
         *
         * @return <tt>HttpRequestPacket</tt>
         */
        public final MultipartEntryPacket build() {
            return packet;
        }

        private Charset getCharset() {
            String charset = null;
            if (packet.contentType != null) {
                charset = ContentType.getCharsetFromContentType(packet.contentType);
            }

            if (charset == null) {
                return Charsets.ASCII_CHARSET;
            }
            
            return Charsets.lookupCharset(charset);
        }

    }
}
