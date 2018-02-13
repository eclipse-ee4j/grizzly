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

package org.glassfish.grizzly;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test thread-local cache.
 * 
 * @author Alexey Stashok
 */
public class ThreadCacheTest {
    @Test
    public void testGet() {
        final ThreadCache.CachedTypeIndex<Object> CACHE_IDX =
            ThreadCache.obtainIndex("testGet", Object.class, 1);

        assertNull(ThreadCache.getFromCache(CACHE_IDX));

        final Object obj = new Object();
        ThreadCache.putToCache(CACHE_IDX, obj);

        for (int i = 0; i < 10; i++) {
            assertEquals(obj, ThreadCache.getFromCache(CACHE_IDX));
        }        
    }

    @Test
    public void testTake() {
        final int size = 5;
        
        final ThreadCache.CachedTypeIndex<Object> CACHE_IDX =
            ThreadCache.obtainIndex("testTake", Object.class, size);

        assertNull(ThreadCache.getFromCache(CACHE_IDX));

        for (int i = 0; i < size * 2; i++) {
            ThreadCache.putToCache(CACHE_IDX, new Object());
        }

        for (int i = 0; i < size; i++) {
            assertNotNull(ThreadCache.takeFromCache(CACHE_IDX));
        }

        assertNull(ThreadCache.takeFromCache(CACHE_IDX));

    }

}
