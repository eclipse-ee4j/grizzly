/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Connection;

/**
 * Abstraction, which represents HTTP content-encoding. Implementation should take care of HTTP content encoding and
 * decoding.
 *
 * @see GZipContentEncoding
 *
 * @author Alexey Stashok
 */
public interface ContentEncoding {

    /**
     * Get the <tt>ContentEncoding</tt> name.
     *
     * @return the <tt>ContentEncoding</tt> name.
     */
    String getName();

    /**
     * Get the <tt>ContentEncoding</tt> aliases.
     *
     * @return the <tt>ContentEncoding</tt> aliases.
     */
    String[] getAliases();

    /**
     * Method should implement the logic, which decides if HTTP packet with the specific {@link HttpHeader} should be
     * decoded using this <tt>ContentEncoding</tt>.
     *
     * @param header HTTP packet header.
     * @return <tt>true</tt>, if this <tt>ContentEncoding</tt> should be used to decode the HTTP packet, or <tt>false</tt>
     * otherwise.
     */
    boolean wantDecode(HttpHeader header);

    /**
     * Method should implement the logic, which decides if HTTP packet with the specific {@link HttpHeader} should be
     * encoded using this <tt>ContentEncoding</tt>.
     *
     * @param header HTTP packet header.
     * @return <tt>true</tt>, if this <tt>ContentEncoding</tt> should be used to encode the HTTP packet, or <tt>false</tt>
     * otherwise.
     */
    boolean wantEncode(HttpHeader header);

    /**
     * Decode HTTP packet content represented by {@link HttpContent}.
     *
     * @param connection {@link Connection}.
     * @param httpContent {@link HttpContent} to decode.
     *
     * @return {@link ParsingResult}, which represents the result of decoding.
     */
    ParsingResult decode(Connection connection, HttpContent httpContent);

    /**
     * Encode HTTP packet content represented by {@link HttpContent}.
     *
     * @param connection {@link Connection}.
     * @param httpContent {@link HttpContent} to encode.
     *
     * @return encoded {@link HttpContent}.
     */
    HttpContent encode(Connection connection, HttpContent httpContent);
}
