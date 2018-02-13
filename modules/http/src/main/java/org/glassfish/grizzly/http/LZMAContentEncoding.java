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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.compression.lzma.LZMADecoder;
import org.glassfish.grizzly.compression.lzma.LZMAEncoder;

public class LZMAContentEncoding implements ContentEncoding {

    private static final String[] ALIASES = { "lzma" };

    public static final String NAME = "lzma";

    private final LZMADecoder decoder;
    private final LZMAEncoder encoder;

    private final EncodingFilter encodingFilter;


    // ------------------------------------------------------------ Constructors


    public LZMAContentEncoding() {
        this(null);
    }

    public LZMAContentEncoding(EncodingFilter encodingFilter) {
        decoder = new LZMADecoder();
        encoder = new LZMAEncoder();
        if (encodingFilter != null) {
            this.encodingFilter = encodingFilter;
        } else {
            this.encodingFilter = new EncodingFilter() {
                @Override
                public boolean applyEncoding(final HttpHeader httpPacket) {
                    return false;
                }

                @Override
                public boolean applyDecoding(final HttpHeader httpPacket) {
                    return true;
                }
            };
        }
    }

    // -------------------------------------------- Methods from ContentEncoding


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String[] getAliases() {
        return ALIASES.clone();
    }
    
    public static String[] getLzmaAliases() {
        return ALIASES.clone();
    }

    @Override
    public boolean wantDecode(HttpHeader header) {
        return encodingFilter.applyDecoding(header);
    }

    @Override
    public boolean wantEncode(HttpHeader header) {
        return encodingFilter.applyEncoding(header);
    }

    @Override
    public ParsingResult decode(Connection connection, HttpContent httpContent) {
        final HttpHeader httpHeader = httpContent.getHttpHeader();
        final Buffer input = httpContent.getContent();
        final TransformationResult<Buffer, Buffer> result =
                decoder.transform(httpHeader, input);

        Buffer remainder = result.getExternalRemainder();

        if (remainder == null || !remainder.hasRemaining()) {
            input.tryDispose();
            remainder = null;
        } else {
            input.shrink();
        }

        try {
            switch (result.getStatus()) {
                case COMPLETE: {
                    httpContent.setContent(result.getMessage());
                    decoder.finish(httpHeader);
                    return ParsingResult.create(httpContent, remainder);
                }

                case INCOMPLETE: {
                    return ParsingResult.create(null, remainder);
                }

                case ERROR: {
                    throw new IllegalStateException("LZMA decode error. Code: "
                            + result.getErrorCode() + " Description: "
                            + result.getErrorDescription());
                }

                default:
                    throw new IllegalStateException("Unexpected status: " +
                            result.getStatus());
            }
        } finally {
            result.recycle();
        }
    }

    @Override
    public HttpContent encode(Connection connection, HttpContent httpContent) {

        final HttpHeader httpHeader = httpContent.getHttpHeader();
        final Buffer input = httpContent.getContent();

        if (httpContent.isLast() && !input.hasRemaining()) {
            return httpContent;
        }

        final TransformationResult<Buffer, Buffer> result =
                encoder.transform(httpContent.getHttpHeader(), input);

        input.tryDispose();

        try {
            switch (result.getStatus()) {
                case COMPLETE:
                    encoder.finish(httpHeader);
                case INCOMPLETE: {
                    Buffer encodedBuffer = result.getMessage();
                    if (encodedBuffer != null) {
                        httpContent.setContent(encodedBuffer);
                        return httpContent;
                    } else {
                        return null;
                    }
                }

                case ERROR: {
                    throw new IllegalStateException("LZMA encode error. Code: "
                            + result.getErrorCode() + " Description: "
                            + result.getErrorDescription());
                }

                default:
                    throw new IllegalStateException("Unexpected status: " +
                            result.getStatus());
            }
        } finally {
            result.recycle();
        }

    }


    // ---------------------------------------------------------- Public Methods


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LZMAContentEncoding that = (LZMAContentEncoding) o;

        if (decoder != null ? !decoder.equals(that.decoder) : that.decoder != null)
            return false;
        if (encoder != null ? !encoder.equals(that.encoder) : that.encoder != null)
            return false;
        if (encodingFilter != null ? !encodingFilter.equals(that.encodingFilter) : that.encodingFilter != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = decoder != null ? decoder.hashCode() : 0;
        result = 31 * result + (encoder != null ? encoder.hashCode() : 0);
        result = 31 * result + (encodingFilter != null ? encodingFilter.hashCode() : 0);
        return result;
    }
}
