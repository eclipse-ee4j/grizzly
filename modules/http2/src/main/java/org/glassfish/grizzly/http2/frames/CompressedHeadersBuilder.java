/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2.frames;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http2.HeadersEncoder;

/**
 * The builder for compressed headers used by {@link HeadersFrame}.
 *
 * @see HeadersFrame
 *
 * @author Alexey Stashok
 */
public final class CompressedHeadersBuilder {

    private final Map<String, String> headers = new HashMap<>();

    private CompressedHeadersBuilder() {
    }

    /**
     * Returns the {@link CompressedHeadersBuilder} instance.
     */
    public static CompressedHeadersBuilder newInstance() {
        return new CompressedHeadersBuilder();
    }

    /**
     * Set the content-length of this header. Applicable only in case of fixed-length HTTP message.
     *
     * @param contentLength the content-length of this {@link HttpPacket}. Applicable only in case of fixed-length HTTP
     * message.
     */
    public CompressedHeadersBuilder contentLength(long contentLength) {
        return header(Header.ContentLength, String.valueOf(contentLength));
    }

    /**
     * Set the content-type of this header.
     *
     * @param contentType the content-type of this {@link HttpPacket}.
     */
    public CompressedHeadersBuilder contentType(String contentType) {
        return header(Header.ContentType, String.valueOf(contentType));
    }

    /**
     * Set the the HTTP method for this request. (e.g. "GET", "POST", "HEAD", etc).
     *
     * @param method the method of this header.
     */
    public CompressedHeadersBuilder method(final Method method) {
        return method(method.getMethodString());
    }

    /**
     * Set the the HTTP method for this request. (e.g. "GET", "POST", "HEAD", etc).
     *
     * @param method the method of this header.
     */
    public CompressedHeadersBuilder method(String method) {
        return header(":method", method);
    }

    /**
     * Set the url-path for required url with "/" prefixed. (See RFC1738 [RFC1738]). For example, for
     * "http://www.google.com/search?q=dogs" the path would be "/search?q=dogs".
     *
     * @param path the path of this header.
     */
    public CompressedHeadersBuilder path(String path) {
        return header(":path", path);
    }

    /**
     * Set the the HTTP version of this request (e.g. "HTTP/1.1").
     *
     * @param version the HTTP version of this header.
     */
    public CompressedHeadersBuilder version(Protocol version) {
        return version(version.getProtocolString());
    }

    /**
     * Set the the HTTP version of this request (e.g. "HTTP/1.1").
     *
     * @param version the HTTP version of this header.
     */
    public CompressedHeadersBuilder version(String version) {
        return header(":version", version);
    }

    /**
     * Set the the host/port (See RFC1738 [RFC1738]) portion of the URL for this request header (e.g.
     * "www.google.com:1234"). This header is the same as the HTTP 'Host' header.
     *
     * @param host the host/port.
     */
    public CompressedHeadersBuilder host(String host) {
        return header(":host", host);
    }

    /**
     * Set the scheme portion of the URL for this request header (e.g. "https").
     *
     * @param scheme the scheme of this header.
     */
    public CompressedHeadersBuilder scheme(String scheme) {
        return header(":scheme", scheme);
    }

    /**
     * Set the HTTP response status code (e.g. 200 or 404).
     *
     * @param status the status of this header.
     */
    public CompressedHeadersBuilder status(int status) {
        return status(String.valueOf(status));
    }

    /**
     * Set the HTTP response status code (e.g. "200" or "200 OK")
     *
     * @param status the status of this header.
     */
    public CompressedHeadersBuilder status(HttpStatus status) {
        final StringBuilder sb = new StringBuilder();
        sb.append(status.getStatusCode()).append(' ')
                .append(new String(status.getReasonPhraseBytes(), org.glassfish.grizzly.http.util.Constants.DEFAULT_HTTP_CHARSET));

        return status(sb.toString());
    }

    /**
     * Set the HTTP response status code (e.g. "200" or "200 OK")
     *
     * @param status the status of this header.
     */
    public CompressedHeadersBuilder status(final String status) {
        return header(":status", status);
    }

    /**
     * Add the HTTP mime header.
     *
     * @param name the mime header name.
     * @param value the mime header value.
     */
    public CompressedHeadersBuilder header(String name, String value) {
        headers.put(name.toLowerCase(Locale.US), value);
        return this;
    }

    /**
     * Add the HTTP mime header.
     *
     * @param header the mime {@link Header}.
     * @param value the mime header value.
     */
    public CompressedHeadersBuilder header(Header header, String value) {
        headers.put(header.getLowerCase(), value);
        return this;
    }

    public Buffer build(final HeadersEncoder encoder) throws IOException {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            encoder.encodeHeader(entry.getKey(), entry.getValue(), null);
        }

        return encoder.flushHeaders();
    }
}
