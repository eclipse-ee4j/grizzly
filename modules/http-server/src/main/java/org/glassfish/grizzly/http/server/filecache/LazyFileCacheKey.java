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

package org.glassfish.grizzly.http.server.filecache;

import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.Header;

/**
 * Lazy {@link FileCacheKey} object.
 * 
 * @author Alexey Stashok
 */
public class LazyFileCacheKey extends FileCacheKey {

    private static final ThreadCache.CachedTypeIndex<LazyFileCacheKey> CACHE_IDX =
                ThreadCache.obtainIndex(LazyFileCacheKey.class, 16);

    private HttpRequestPacket request;
    private boolean isInitialized;
    private int hashCode;


    // ------------------------------------------------------------ Constructors

    
    private LazyFileCacheKey(final HttpRequestPacket request) {
        this.request = request;
    }


    // ----------------------------------------------- Methods from FileCacheKey


    @Override
    protected String getHost() {
        if (!isInitialized) {
            initialize();
        }
        
        return super.getHost();
    }

    @Override
    protected String getUri() {
        if (!isInitialized) {
            initialize();
        }
        
        return super.getUri();
    }


    // -------------------------------------------------- Methods from Cacheable


    @Override
    public void recycle() {
        host = null;
        uri = null;
        isInitialized = false;
        request = null;
        hashCode = 0;
        ThreadCache.putToCache(CACHE_IDX, this);
    }


    // ---------------------------------------------------------- Public Methods


    public static LazyFileCacheKey create(final HttpRequestPacket request) {
        final LazyFileCacheKey key =
                ThreadCache.takeFromCache(CACHE_IDX);
        if (key != null) {
            key.request = request;
            return key;
        }

        return new LazyFileCacheKey(request);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }        
        final FileCacheKey other = (FileCacheKey) obj;
        
        final String otherHost = other.host;
        final DataChunk hostDC = getHostLazy();
        if ((hostDC == null || hostDC.isNull()) ? (otherHost != null) : !hostDC.equals(otherHost)) {
            return false;
        }

        final String otherUri = other.uri;
        final DataChunk uriDC = getUriLazy();
        if ((uriDC == null || uriDC.isNull()) ? (otherUri != null) : !uriDC.equals(otherUri)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int hash = 3;
            final DataChunk hostDC = getHostLazy();
            final DataChunk uriDC = getUriLazy();
        
            hash = 23 * hash + (hostDC != null ? hostDC.hashCode() : 0);
            hash = 23 * hash + (uriDC != null ? uriDC.hashCode() : 0);
            hashCode = hash;
        }
        return hashCode;
    }


    // --------------------------------------------------------- Private Methods

    
    private void initialize() {
        isInitialized = true;
        host = request.getHeader(Header.Host);
        uri = request.getRequestURI();
    }
    
    private DataChunk getHostLazy() {
        return request.getHeaders().getValue(Header.Host);
    }
    
    private DataChunk getUriLazy() {
        return request.getRequestURIRef().getRequestURIBC();
    }
}
