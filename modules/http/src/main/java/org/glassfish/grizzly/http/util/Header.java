/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.util;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.glassfish.grizzly.utils.Charsets;

/**
 * Enumeration of all headers as defined in <code>RFC 2616</code>.
 *
 * @since 2.1.2
 */
public enum Header {

    Accept("Accept"), AcceptCharset("Accept-Charset"), AcceptEncoding("Accept-Encoding"), AcceptRanges("Accept-Ranges"), Age("Age"), Allow("Allow"),
    Authorization("Authorization"), CacheControl("Cache-Control"), Cookie("Cookie"), Connection("Connection"), ContentDisposition("Content-Disposition"),
    ContentEncoding("Content-Encoding"), ContentLanguage("Content-Language"), ContentLength("Content-Length"), ContentLocation("Content-Location"),
    ContentMD5("Content-MD5"), ContentRange("Content-Range"), ContentType("Content-Type"), Date("Date"), ETag("ETag"), Expect("Expect"), Expires("Expires"),
    From("From"), Host("Host"), IfMatch("If-Match"), IfModifiedSince("If-Modified-Since"), IfNoneMatch("If-None-Match"), IfRange("If-Range"),
    IfUnmodifiedSince("If-Unmodified-Since"), KeepAlive("Keep-Alive"), LastModified("Last-Modified"), Location("Location"), MaxForwards("Max-Forwards"),
    Pragma("Pragma"), ProxyAuthenticate("Proxy-Authenticate"), ProxyAuthorization("Proxy-Authorization"), ProxyConnection("Proxy-Connection"), Range("Range"),
    @SuppressWarnings("SpellCheckingInspection")
    Referer("Referer"), RetryAfter("Retry-After"), Server("Server"), SetCookie("Set-Cookie"), TE("TE"), Trailer("Trailer"),
    TransferEncoding("Transfer-Encoding"), Upgrade("Upgrade"), UserAgent("User-Agent"), Vary("Vary"), Via("Via"), Warnings("Warning"),
    WWWAuthenticate("WWW-Authenticate"), XPoweredBy("X-Powered-By"), HTTP2Settings("HTTP2-Settings");

    // ----------------------------------------------------------------- Statics

    private static final Map<String, Header> VALUES = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    static {
        for (final Header h : Header.values()) {
            VALUES.put(h.toString(), h);
        }
    }

    // --------------------------------------------------------- Per Enum Fields

    private final byte[] headerNameBytes;
    private final byte[] headerNameLowerCaseBytes;
    private final String headerName;
    private final String headerNameLowerCase;
    private final int length;

    // ------------------------------------------------------------ Constructors

    Header(final String headerName) {
        this.headerName = headerName;
        headerNameBytes = headerName.getBytes(Charsets.ASCII_CHARSET);

        this.headerNameLowerCase = headerName.toLowerCase(Locale.ENGLISH);
        headerNameLowerCaseBytes = headerNameLowerCase.getBytes(Charsets.ASCII_CHARSET);

        length = headerNameBytes.length;
    }

    // ---------------------------------------------------------- Public Methods

    /**
     * <p>
     * Returns the byte representation of this header encoded using <code>ISO-8859-1</code>.
     * </p>
     *
     * @return the byte representation of this header encoded using <code>ISO-8859-1</code>.
     */
    public final byte[] getBytes() {
        return headerNameBytes;
    }

    /**
     * <p>
     * Returns the lower-case {@link String} representation of this header.
     * </p>
     *
     * @return the lower-case {@link String} representation of this header
     */
    public final String getLowerCase() {
        return headerNameLowerCase;
    }

    /**
     * <p>
     * Returns the lower-case byte representation of this header encoded using <code>ISO-8859-1</code>.
     * </p>
     *
     * @return the lower-case byte representation of this header encoded using <code>ISO-8859-1</code>.
     */
    public final byte[] getLowerCaseBytes() {
        return headerNameLowerCaseBytes;
    }

    /**
     * <p>
     * Returns the length this header encoded using <code>ISO-8859-1</code>.
     * </p>
     *
     * @return the length this header encoded using <code>ISO-8859-1</code>.
     */
    public final int getLength() {
        return length;
    }

    /**
     * <p>
     * Returns the name of the header properly hyphenated if necessary.
     * </p>
     *
     * @return Returns the name of the header properly hyphenated if necessary.
     */
    @Override
    public final String toString() {
        return headerName;
    }

    /**
     * <p>
     * Returns the US-ASCII encoded byte representation of this <code>Header</code>.
     * </p>
     * 
     * @return the US-ASCII encoded byte representation of this <code>Header</code>.
     */
    public final byte[] toByteArray() {
        return headerNameBytes;
    }

    /**
     * <p>
     * Attempts to find a HTTP header by it's standard textual definition which may differ from value value returned by
     * {@link #name}. Note that this search is case insensitive.
     * </p>
     *
     * @param name the name of the <code>Header</code> to attempt to find.
     *
     * @return the <code>Header</code> for the specified text representation. If no <code>Header</code> matches or if the
     * specified argument is <code>null/zero-length</code>, this method returns </code>null</code>.
     */
    public static Header find(final String name) {

        if (name == null || name.isEmpty()) {
            return null;
        }
        return VALUES.get(name);

    }

}
