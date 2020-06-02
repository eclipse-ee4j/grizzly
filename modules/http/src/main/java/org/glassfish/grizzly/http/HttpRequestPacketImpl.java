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

/**
 *
 * @author Alexey Stashok
 */
class HttpRequestPacketImpl extends HttpRequestPacket {
    private static final ThreadCache.CachedTypeIndex<HttpRequestPacketImpl> CACHE_IDX = ThreadCache.obtainIndex(HttpRequestPacketImpl.class, 16);

    public static HttpRequestPacketImpl create() {
        final HttpRequestPacketImpl httpRequestImpl = ThreadCache.takeFromCache(CACHE_IDX);
        if (httpRequestImpl != null) {
            return httpRequestImpl;
        }

        return new HttpRequestPacketImpl() {
            @Override
            public void recycle() {
                super.recycle();
                ThreadCache.putToCache(CACHE_IDX, this);
            }
        };
    }

    private final ProcessingState processingState;

    protected HttpRequestPacketImpl() {
        this.processingState = new ProcessingState();
        isExpectContent = true;
    }

    @Override
    public ProcessingState getProcessingState() {
        return processingState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
//        headerParsingState.recycle();
//        contentParsingState.recycle();
        processingState.recycle();
//        isHeaderParsed = false;
        isExpectContent = true;
        super.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recycle() {
        if (isExpectContent()) {
            return;
        }
        reset();
//        ThreadCache.putToCache(CACHE_IDX, this);
    }
}
