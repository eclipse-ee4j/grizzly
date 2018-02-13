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

/**
 * Abstraction, which represents HTTP transfer-encoding.
 * The implementation should take care about possible HTTP content fragmentation.
 *
 * @see FixedLengthTransferEncoding
 * @see ChunkedTransferEncoding
 * 
 * @author Alexey Stashok
 */
public interface TransferEncoding {
    /**
     * Return <tt>true</tt> if this encoding should be used to parse the
     * content of the passed {@link HttpHeader}, or <tt>false</tt> otherwise.
     * 
     * @param httpPacket {@link HttpHeader}.
     * @return <tt>true</tt> if this encoding should be used to parse the
     * content of the passed {@link HttpHeader}, or <tt>false</tt> otherwise.
     */
    boolean wantDecode(HttpHeader httpPacket);

    /**
     * Return <tt>true</tt> if this encoding should be used to serialize the
     * content of the passed {@link HttpHeader}, or <tt>false</tt> otherwise.
     *
     * @param httpPacket {@link HttpHeader}.
     * @return <tt>true</tt> if this encoding should be used to serialize the
     * content of the passed {@link HttpHeader}, or <tt>false</tt> otherwise.
     */
    boolean wantEncode(HttpHeader httpPacket);

    /**
     * This method will be called by {@link HttpCodecFilter} to let
     * <tt>TransferEncoding</tt> prepare itself for the content serialization.
     * At this time <tt>TransferEncoding</tt> is able to change, update HTTP
     * packet headers.
     *
     * @param ctx {@link FilterChainContext}
     * @param httpHeader HTTP packet headers.
     * @param content ready HTTP content (might be null).
     */
    void prepareSerialize(FilterChainContext ctx,
                          HttpHeader httpHeader,
                          HttpContent content);

    /**
     * Parse HTTP packet payload, represented by {@link Buffer} using specific
     * transfer encoding.
     *
     * @param ctx {@link FilterChainContext}
     * @param httpPacket {@link HttpHeader} with parsed headers.
     * @param buffer {@link Buffer} HTTP message payload.
     * @return {@link ParsingResult}
     */
    ParsingResult parsePacket(FilterChainContext ctx,
                              HttpHeader httpPacket, Buffer buffer);

    /**
     * Serialize HTTP packet payload, represented by {@link HttpContent}
     * using specific transfer encoding.
     *
     * @param ctx {@link FilterChainContext}
     * @param httpContent {@link HttpContent} with parsed {@link HttpContent#getHttpHeader()}.
     *
     * @return serialized {@link Buffer}
     */
    Buffer serializePacket(FilterChainContext ctx,
                           HttpContent httpContent);
}
