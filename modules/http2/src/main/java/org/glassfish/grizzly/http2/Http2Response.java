/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.ProcessingState;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.Header;

/**
 *
 * @author oleksiys
 */
class Http2Response extends HttpResponsePacket implements Http2Header {
    private static final ThreadCache.CachedTypeIndex<Http2ResponseRecyclable> CACHE_IDX =
            ThreadCache.obtainIndex(Http2ResponseRecyclable.class, 2);

    public static Http2Response create() {
        Http2Response http2Response =
                ThreadCache.takeFromCache(CACHE_IDX);
        if (http2Response == null) {
            http2Response = new Http2Response();
        }
        
        return http2Response;
    }
    
    /**
     * Char encoding parsed flag.
     */
    private boolean contentTypeParsed;

    @Override
    public ProcessingState getProcessingState() {
        return getRequest().getProcessingState();
    }
    
    @Override
    public Http2Stream getHttp2Stream() {
        return Http2Stream.getStreamFor(this);
    }
    
    @Override
    public String getCharacterEncoding() {
        if (!contentTypeParsed) {
            parseContentTypeHeader();
        }

        return super.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(final String charset) {
        if (!contentTypeParsed) {
            parseContentTypeHeader();
        }

        super.setCharacterEncoding(charset);
    }

    @Override
    public String getContentType() {
        if (!contentTypeParsed) {
            parseContentTypeHeader();
        }

        return super.getContentType();
    }

    private void parseContentTypeHeader() {
        contentTypeParsed = true;

        if (!contentType.isSet()) {
            final DataChunk dc = headers.getValue(Header.ContentType);

            if (dc != null && !dc.isNull()) {
                setContentType(dc.toString());
            }
        }
    }
    
    @Override
    public void setExpectContent(final boolean isExpectContent) {
        super.setExpectContent(isExpectContent);
    }
    
    @Override
    protected void reset() {
        contentTypeParsed = false;
        
        super.reset();
    }

    private static class Http2ResponseRecyclable extends Http2Response {
        @Override
        public void recycle() {
            reset();

            ThreadCache.putToCache(CACHE_IDX, this);
        }
    }
}
