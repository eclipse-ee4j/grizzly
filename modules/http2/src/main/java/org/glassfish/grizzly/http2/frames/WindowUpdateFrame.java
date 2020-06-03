/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.Map;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.memory.MemoryManager;

public class WindowUpdateFrame extends Http2Frame {

    private static final ThreadCache.CachedTypeIndex<WindowUpdateFrame> CACHE_IDX = ThreadCache.obtainIndex(WindowUpdateFrame.class, 8);

    public static final int TYPE = 8;

    private int windowSizeIncrement;

    // ------------------------------------------------------------ Constructors

    private WindowUpdateFrame() {
    }

    // ---------------------------------------------------------- Public Methods

    static WindowUpdateFrame create() {
        WindowUpdateFrame frame = ThreadCache.takeFromCache(CACHE_IDX);
        if (frame == null) {
            frame = new WindowUpdateFrame();
        }
        return frame;
    }

    public static Http2Frame fromBuffer(final int flags, final int streamId, final Buffer frameBuffer) {
        WindowUpdateFrame frame = create();
        frame.setFlags(flags);
        frame.setStreamId(streamId);
        frame.setFrameBuffer(frameBuffer);

        frame.windowSizeIncrement = frameBuffer.getInt() & 0x7fffffff;

        return frame;
    }

    public static WindowUpdateFrameBuilder builder() {
        return new WindowUpdateFrameBuilder();
    }

    public int getWindowSizeIncrement() {
        return windowSizeIncrement;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("WindowUpdateFrame {").append(headerToString()).append(", windowSizeIncrement=").append(windowSizeIncrement).append('}');

        return sb.toString();
    }

    @Override
    protected int calcLength() {
        return 4;
    }

    @Override
    protected Map<Integer, String> getFlagNamesMap() {
        return Collections.emptyMap();
    }
    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        if (DONT_RECYCLE) {
            return;
        }

        windowSizeIncrement = 0;

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
        final Buffer buffer = memoryManager.allocate(FRAME_HEADER_SIZE + 4);

        serializeFrameHeader(buffer);
        buffer.putInt(windowSizeIncrement & 0x7fffffff);

        buffer.trim();

        return buffer;
    }

    // ---------------------------------------------------------- Nested Classes

    public static class WindowUpdateFrameBuilder extends Http2FrameBuilder<WindowUpdateFrameBuilder> {

        private int windowSizeIncrement;

        // -------------------------------------------------------- Constructors

        protected WindowUpdateFrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods

        public WindowUpdateFrameBuilder windowSizeIncrement(final int windowSizeIncrement) {
            this.windowSizeIncrement = windowSizeIncrement;
            return this;
        }

        @Override
        public WindowUpdateFrame build() {
            final WindowUpdateFrame frame = WindowUpdateFrame.create();
            setHeaderValuesTo(frame);
            frame.windowSizeIncrement = windowSizeIncrement;

            return frame;
        }

        // --------------------------------------- Methods from Http2FrameBuilder

        @Override
        protected WindowUpdateFrameBuilder getThis() {
            return this;
        }

    } // END WindowUpdateFrameBuilder

}
