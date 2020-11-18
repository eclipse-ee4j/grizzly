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

import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.HeaderValue;
import org.glassfish.grizzly.http.util.MimeHeaders;

/**
 * {@link HttpContent} message, which represents HTTP trailer message. Applicable only for chunked HTTP messages.
 *
 * @author Alexey Stashok
 */
public class HttpTrailer extends HttpContent implements MimeHeadersPacket {
    private static final ThreadCache.CachedTypeIndex<HttpTrailer> CACHE_IDX = ThreadCache.obtainIndex(HttpTrailer.class, 16);

    /**
     * @return <tt>true</tt> if passed {@link HttpContent} is a <tt>HttpTrailder</tt>.
     */
    public static boolean isTrailer(HttpContent httpContent) {
        return HttpTrailer.class.isAssignableFrom(httpContent.getClass());
    }

    public static HttpTrailer create() {
        return create(null);
    }

    public static HttpTrailer create(HttpHeader httpHeader) {
        final HttpTrailer httpTrailer = ThreadCache.takeFromCache(CACHE_IDX);
        if (httpTrailer != null) {
            httpTrailer.httpHeader = httpHeader;
            return httpTrailer;
        }

        return new HttpTrailer(httpHeader);
    }

    /**
     * Returns {@link HttpTrailer} builder.
     *
     * @return {@link Builder}.
     */
    public static Builder builder(HttpHeader httpHeader) {
        return new Builder().httpHeader(httpHeader);
    }

    private MimeHeaders trailers;

    protected HttpTrailer(HttpHeader httpHeader) {
        super(httpHeader);
        trailers = new MimeHeaders();
        trailers.mark();
    }

    /**
     * Always true <tt>true</tt> for the trailer message.
     *
     * @return Always true <tt>true</tt> for the trailer message.
     */
    @Override
    public final boolean isLast() {
        return true;
    }

    // -------------------- Headers --------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public MimeHeaders getHeaders() {
        return trailers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(final String name) {
        return trailers.getHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(final Header header) {
        return trailers.getHeader(header);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final String name, final String value) {
        if (name == null || value == null) {
            return;
        }
        trailers.setValue(name).setString(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final String name, final HeaderValue value) {
        if (name == null || value == null || !value.isSet()) {
            return;
        }
        value.serializeToDataChunk(trailers.setValue(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final Header header, final String value) {
        if (header == null || value == null) {
            return;
        }
        trailers.setValue(header).setString(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final Header header, final HeaderValue value) {
        if (header == null || value == null || !value.isSet()) {
            return;
        }
        value.serializeToDataChunk(trailers.setValue(header));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final String name, final String value) {
        if (name == null || value == null) {
            return;
        }
        trailers.addValue(name).setString(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final String name, final HeaderValue value) {
        if (name == null || value == null || !value.isSet()) {
            return;
        }
        value.serializeToDataChunk(trailers.setValue(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final Header header, final String value) {
        if (header == null || value == null) {
            return;
        }
        final DataChunk c = trailers.addValue(header);
        if (c != null) {
            c.setString(value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final Header header, final HeaderValue value) {
        if (header == null || value == null || !value.isSet()) {
            return;
        }
        value.serializeToDataChunk(trailers.setValue(header));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsHeader(final String name) {
        return trailers.contains(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsHeader(final Header header) {
        return trailers.contains(header);
    }

    /**
     * Set the mime trailers.
     * 
     * @param trailers {@link MimeHeaders}.
     */
    @SuppressWarnings("unused")
    protected void setTrailers(final MimeHeaders trailers) {
        this.trailers = trailers;
        this.trailers.mark();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        trailers.recycle();
        trailers.mark();
        super.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recycle() {
        reset();
        ThreadCache.putToCache(CACHE_IDX, this);
    }

    /**
     * <tt>HttpTrailer</tt> message builder.
     */
    public static final class Builder extends HttpContent.Builder<Builder> {

        private MimeHeaders mimeTrailers;

        protected Builder() {
        }

        /**
         * Set the mime trailers.
         *
         * This method will overwrite any trailers provided via {@link #header(String, String)} before this invocation.
         *
         * @param mimeTrailers {@link MimeHeaders}.
         */
        public Builder headers(MimeHeaders mimeTrailers) {
            this.mimeTrailers = mimeTrailers;
            mimeTrailers.mark(); // this is idempotent
            return this;
        }

        /**
         * Add the HTTP mime header.
         *
         * @param name the mime header name.
         * @param value the mime header value.
         */
        public Builder header(String name, String value) {
            if (mimeTrailers == null) {
                mimeTrailers = new MimeHeaders();
                mimeTrailers.mark();
            }
            final DataChunk c = mimeTrailers.addValue(name);
            if (c != null) {
                c.setString(value);
            }
            return this;
        }

        /**
         * Build the <tt>HttpTrailer</tt> message.
         *
         * @return <tt>HttpTrailer</tt>
         */
        @Override
        public HttpTrailer build() {
            HttpTrailer trailer = (HttpTrailer) super.build();
            if (mimeTrailers != null) {
                trailer.trailers = mimeTrailers;
            }
            return trailer;
        }

        @Override
        protected HttpContent create() {
            return HttpTrailer.create();
        }
    }
}
