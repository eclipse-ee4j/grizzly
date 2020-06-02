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

package org.glassfish.grizzly.memory;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ThreadCache;

/**
 *
 * @author oleksiys
 */
public final class BufferArray extends AbstractBufferArray<Buffer> {

    private static final ThreadCache.CachedTypeIndex<BufferArray> CACHE_IDX = ThreadCache.obtainIndex(BufferArray.class,
            Integer.getInteger(BufferArray.class.getName() + "ba-cache-size", 4));

    public static BufferArray create() {
        final BufferArray array = ThreadCache.takeFromCache(CACHE_IDX);
        if (array != null) {
            return array;
        }

        return new BufferArray();
    }

    private BufferArray() {
        super(Buffer.class);
    }

    @Override
    public void recycle() {
        super.recycle();

        ThreadCache.putToCache(CACHE_IDX, this);
    }

    @Override
    protected void setPositionLimit(final Buffer buffer, final int position, final int limit) {
        Buffers.setPositionLimit(buffer, position, limit);
    }

    @Override
    protected int getPosition(final Buffer buffer) {
        return buffer.position();
    }

    @Override
    protected int getLimit(final Buffer buffer) {
        return buffer.limit();
    }
}
