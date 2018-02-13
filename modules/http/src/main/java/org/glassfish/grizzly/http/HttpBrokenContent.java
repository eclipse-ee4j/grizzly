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
import org.glassfish.grizzly.ThreadCache;

/**
 * {@link HttpContent} message, which represents broken HTTP content.
 * {@link #isLast()} is always returns <tt>true</tt>,
 * {@link #getContent()} always throws {@link HttpBrokenContentException()}.
 * 
 * @see HttpContent#isBroken(org.glassfish.grizzly.http.HttpContent)
 * 
 * @author Alexey Stashok
 */
public class HttpBrokenContent extends HttpContent {
    private static final ThreadCache.CachedTypeIndex<HttpBrokenContent> CACHE_IDX =
            ThreadCache.obtainIndex(HttpBrokenContent.class, 1);

    public static HttpBrokenContent create() {
        return create(null);
    }

    public static HttpBrokenContent create(final HttpHeader httpHeader) {
        final HttpBrokenContent httpBrokenContent =
                ThreadCache.takeFromCache(CACHE_IDX);
        if (httpBrokenContent != null) {
            httpBrokenContent.httpHeader = httpHeader;
            return httpBrokenContent;
        }

        return new HttpBrokenContent(httpHeader);
    }


    /**
     * Returns {@link HttpTrailer} builder.
     *
     * @return {@link Builder}.
     */
    public static Builder builder(final HttpHeader httpHeader) {
        return new Builder().httpHeader(httpHeader);
    }

    private Throwable exception;
    
    protected HttpBrokenContent(final HttpHeader httpHeader) {
        super(httpHeader);
    }

    /**
     * Returns {@link Throwable}, which describes the error.
     * @return {@link Throwable}, which describes the error.
     */
    public Throwable getException() {
        return exception;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer getContent() {
        throw exception instanceof HttpBrokenContentException ?
                (HttpBrokenContentException) exception :
                new HttpBrokenContentException(exception);
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        this.exception = null;
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

        private Throwable cause;

        protected Builder() {
        }

        /**
         * Set the exception.
         * @param cause {@link Throwable}.
         */
        public final Builder error(final Throwable cause) {
            this.cause = cause;
            return this;
        }

        /**
         * Build the <tt>HttpTrailer</tt> message.
         *
         * @return <tt>HttpTrailer</tt>
         */
        @Override
        public final HttpBrokenContent build() {
            HttpBrokenContent httpBrokenContent = (HttpBrokenContent) super.build();
            if (cause == null) {
                throw new IllegalStateException("No cause specified");
            }
            httpBrokenContent.exception = cause;
            return httpBrokenContent;
        }

        @Override
        protected HttpContent create() {
            return HttpBrokenContent.create();
        }
    }
}
