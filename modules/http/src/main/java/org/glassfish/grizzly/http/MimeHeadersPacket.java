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

import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HeaderValue;
import org.glassfish.grizzly.http.util.MimeHeaders;

/**
 * Common interface for {@link HttpPacket}s, which contain mimeheaders.
 * 
 * @author Alexey Stashok
 */
public interface MimeHeadersPacket {
    /**
     * Get all {@link MimeHeaders}, associated with the <tt>HttpHeader</tt>.
     *
     * @return all {@link MimeHeaders}, associated with the <tt>HttpHeader</tt>
     */
    MimeHeaders getHeaders();

    /**
     * Get the value, of the specific HTTP mime header.
     * @param name the mime header name
     *
     * @return the value, of the specific HTTP mime header
     */
    String getHeader(String name);

    /**
     * Get the value, of the specific HTTP mime header.
     * @param header the mime {@link Header}
     *
     * @return the value, of the specific HTTP mime header
     *
     * @since 2.1.2
     */
    String getHeader(final Header header);

    /**
     * Set the value, of the specific HTTP mime header.
     *
     * @param name the mime header name
     * @param value the mime header value
     */
    void setHeader(String name, String value);

    /**
     * Set the value, of the specific HTTP mime header.
     *
     * @param name the mime header name
     * @param value the mime header value
     * 
     * @since 2.3.8
     */
    void setHeader(String name, HeaderValue value);
    
    /**
     * Set the value, of the specific HTTP mime header.
     *
     * @param header the mime {@link Header}
     * @param value the mime header value
     *
     * @since 2.1.2
     */
    void setHeader(final Header header, String value);

    /**
     * Set the value, of the specific HTTP mime header.
     *
     * @param header the mime {@link Header}
     * @param value the mime header value
     *
     * @since 2.3.8
     */
    void setHeader(final Header header, HeaderValue value);
    
    /**
     * Add the HTTP mime header.
     *
     * @param name the mime header name
     * @param value the mime header value
     */
    void addHeader(String name, String value);

    /**
     * Add the HTTP mime header.
     *
     * @param name the mime header name
     * @param value the mime header value
     * 
     * @since 2.3.8
     */
    void addHeader(String name, HeaderValue value);
    
    /**
     * Add the HTTP mime header.
     *
     * @param header the mime {@link Header}
     * @param value the mime header value
     *
     * @since 2.1.2
     */
    void addHeader(final Header header, final String value);

    /**
     * Add the HTTP mime header.
     *
     * @param header the mime {@link Header}
     * @param value the mime header value
     *
     * @since 2.3.8
     */
    void addHeader(final Header header, final HeaderValue value);
    
    /**
     * Returns <tt>true</tt>, if the mime header with the specific name is present
     * among the <tt>HttpHeader</tt> mime headers, or <tt>false</tt> otherwise.
     *
     * @param name the mime header name
     *
     * @return <tt>true</tt>, if the mime header with the specific name is present
     * among the <tt>HttpHeader</tt> mime headers, or <tt>false</tt> otherwise
     */
    boolean containsHeader(String name);

    /**
     * Returns <tt>true</tt>, if the mime {@link Header} is present
     * among the <tt>HttpHeader</tt> mime headers, otherwise returns <tt>false</tt>.
     *
     * @param header the mime {@link Header}
     *
     * @return <tt>true</tt>, if the mime {@link Header} is present
     * among the <tt>HttpHeader</tt> mime headers, otherwise returns <tt>false</tt>
     *
     * @since 2.1.2
     */
    boolean containsHeader(final Header header);

}
