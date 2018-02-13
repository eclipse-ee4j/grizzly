/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpCodecFilter.ContentParsingState;

/**
 * Fixed length transfer encoding implementation.
 *
 * @see TransferEncoding
 *
 * @author Alexey Stashok
 */
public final class FixedLengthTransferEncoding implements TransferEncoding {
    public FixedLengthTransferEncoding() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wantDecode(HttpHeader httpPacket) {
        final long contentLength = httpPacket.getContentLength();

        return (contentLength != -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean wantEncode(HttpHeader httpPacket) {
        final long contentLength = httpPacket.getContentLength();

        return (contentLength != -1);
    }

    @Override
    public void prepareSerialize(FilterChainContext ctx,
                                 HttpHeader httpHeader,
                                 HttpContent httpContent) {
        final int defaultContentLength = httpContent != null ?
            httpContent.getContent().remaining() : -1;
        
        httpHeader.makeContentLengthHeader(defaultContentLength);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({"UnusedDeclaration"})
    public ParsingResult parsePacket(FilterChainContext ctx,
                                     HttpHeader httpPacket,
                                     Buffer input) {

        final HttpPacketParsing httpPacketParsing = (HttpPacketParsing) httpPacket;
        // Get HTTP content parsing state
        final ContentParsingState contentParsingState =
                httpPacketParsing.getContentParsingState();


        if (contentParsingState.chunkRemainder == -1) {
            // if we have just parsed a HTTP message header
            // assign chunkRemainder to the HTTP message content length
            contentParsingState.chunkRemainder = httpPacket.getContentLength();
        }

        Buffer remainder = null;

        final long thisPacketRemaining = contentParsingState.chunkRemainder;
        final int available = input.remaining();

        if (available > thisPacketRemaining) {
            // if input Buffer has part of the next HTTP message - slice it
            remainder = input.slice(
                    (int) (input.position() + thisPacketRemaining), input.limit());
            input.limit((int) (input.position() + thisPacketRemaining));
        }

        // recalc. the HTTP message remaining bytes
        contentParsingState.chunkRemainder -= input.remaining();

        final boolean isLast = (contentParsingState.chunkRemainder == 0);

        return ParsingResult.create(httpPacket.httpContentBuilder().content(input)
                .last(isLast).build(), remainder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer serializePacket(FilterChainContext ctx, HttpContent httpContent) {
        return httpContent.getContent();
    }
}
