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

package org.glassfish.grizzly.http.server.filecache;

import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.ThreadCache;

/**
 * The entry key in the file cache map.
 * 
 * @author Alexey Stashok
 */
public class FileCacheKey implements Cacheable {

    private static final ThreadCache.CachedTypeIndex<FileCacheKey> CACHE_IDX =
                    ThreadCache.obtainIndex(FileCacheKey.class, 16);

    protected String host;
    protected String uri;


    // ------------------------------------------------------------ Constructors


    protected FileCacheKey() { }

    protected FileCacheKey(final String host, final String uri) {
        this.host = host;
        this.uri = uri;
    }


    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        host = null;
        uri = null;
        ThreadCache.putToCache(CACHE_IDX, this);
    }


    // ---------------------------------------------------------- Public Methods


    public static FileCacheKey create(final String host, final String uri) {
        final FileCacheKey key =
                ThreadCache.takeFromCache(CACHE_IDX);
        if (key != null) {
            key.host = host;
            key.uri = uri;
            return key;
        }

        return new FileCacheKey(host, uri);
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }        
        final FileCacheKey other = (FileCacheKey) obj;
        
        final String otherHost = other.host;
        if ((this.host == null) ? (otherHost != null) : !this.host.equals(otherHost)) {
            return false;
        }

        final String otherUri = other.uri;
        if ((this.uri == null) ? (otherUri != null) : !this.uri.equals(otherUri)) {
            return false;
        }
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 23 * hash + (this.host != null ? this.host.hashCode() : 0);
        hash = 23 * hash + (this.uri != null ? this.uri.hashCode() : 0);
        return hash;
    }


    // ------------------------------------------------------- Protected Methods


    protected String getHost() {
        return host;
    }

    protected String getUri() {
        return uri;
    }

}
