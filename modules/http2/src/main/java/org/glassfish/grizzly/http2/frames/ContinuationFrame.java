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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

public class ContinuationFrame extends HeaderBlockFragment {

    private static final ThreadCache.CachedTypeIndex<ContinuationFrame> CACHE_IDX = ThreadCache.obtainIndex(ContinuationFrame.class, 8);

    public static final int TYPE = 9;

    // ------------------------------------------------------------ Constructors

    private ContinuationFrame() {
    }

    // ---------------------------------------------------------- Public Methods

    public static ContinuationFrame fromBuffer(final int flags, final int streamId, final Buffer buffer) {
        final ContinuationFrame frame = create();
        frame.setFlags(flags);
        frame.setStreamId(streamId);
        frame.compressedHeaders = buffer.split(buffer.position());
        frame.setFrameBuffer(buffer);

        return frame;
    }

    static ContinuationFrame create() {
        ContinuationFrame frame = ThreadCache.takeFromCache(CACHE_IDX);
        if (frame == null) {
            frame = new ContinuationFrame();
        }

        return frame;
    }

    public static ContinuationFrameBuilder builder() {
        return new ContinuationFrameBuilder();
    }

    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        if (DONT_RECYCLE) {
            return;
        }

        super.recycle();
        ThreadCache.putToCache(CACHE_IDX, this);
    }

    // -------------------------------------------------- Methods from Http2Frame
    @Override
    public int getType() {
        return TYPE;
    }

    @Override
    public Buffer toBuffer(final MemoryManager memoryManager) {

        final Buffer buffer = memoryManager.allocate(FRAME_HEADER_SIZE);

        serializeFrameHeader(buffer);

        buffer.trim();
        final CompositeBuffer cb = CompositeBuffer.newBuffer(memoryManager, buffer, compressedHeaders);

        cb.allowBufferDispose(true);
        cb.allowInternalBuffersDispose(true);
        return cb;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ContinuationFrame {").append(headerToString()).append(", compressedHeaders=").append(compressedHeaders).append('}');
        return sb.toString();
    }

    @Override
    protected int calcLength() {
        return compressedHeaders.remaining();
    }

    // ---------------------------------------------------------- Nested Classes

    public static class ContinuationFrameBuilder extends HeaderBlockFragmentBuilder<ContinuationFrameBuilder> {

        // -------------------------------------------------------- Constructors

        protected ContinuationFrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods

        @Override
        public ContinuationFrame build() {
            final ContinuationFrame frame = ContinuationFrame.create();
            setHeaderValuesTo(frame);

            frame.compressedHeaders = compressedHeaders;

            return frame;
        }

        // --------------------------------------- Methods from HeaderBlockFragmentBuilder

        @Override
        protected ContinuationFrameBuilder getThis() {
            return this;
        }

    } // END ContinuationFrameBuilder

}
