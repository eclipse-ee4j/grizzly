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

import java.util.HashMap;
import java.util.Map;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.memory.MemoryManager;

public class PingFrame extends Http2Frame {

    private static final ThreadCache.CachedTypeIndex<PingFrame> CACHE_IDX = ThreadCache.obtainIndex(PingFrame.class, 8);

    public static final int TYPE = 6;

    public static final byte ACK_FLAG = 0x1;

    static final Map<Integer, String> FLAG_NAMES_MAP = new HashMap<>(2);

    static {
        FLAG_NAMES_MAP.put((int) ACK_FLAG, "ACK");
    }

    private long opaqueData;

    // ------------------------------------------------------------ Constructors

    private PingFrame() {
    }

    // ---------------------------------------------------------- Public Methods

    static PingFrame create() {
        PingFrame frame = ThreadCache.takeFromCache(CACHE_IDX);
        if (frame == null) {
            frame = new PingFrame();
        }
        return frame;
    }

    public static Http2Frame fromBuffer(final int flags, final int streamId, final Buffer frameBuffer) {
        PingFrame frame = create();
        frame.setFlags(flags);
        frame.setStreamId(streamId);
        frame.setFrameBuffer(frameBuffer);
        if (frameBuffer.remaining() != 8) {
            frame.length = frameBuffer.remaining();
        } else {
            frame.opaqueData = frameBuffer.getLong();
        }

        return frame;
    }

    public static PingFrameBuilder builder() {
        return new PingFrameBuilder();
    }

    public long getOpaqueData() {
        return opaqueData;
    }

    public boolean isAckSet() {
        return isFlagSet(ACK_FLAG);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("PingFrame {").append(headerToString()).append(", opaqueData=").append(opaqueData).append('}');
        return sb.toString();
    }

    @Override
    protected int calcLength() {
        return 8;
    }

    @Override
    protected Map<Integer, String> getFlagNamesMap() {
        return FLAG_NAMES_MAP;
    }

    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        if (DONT_RECYCLE) {
            return;
        }

        opaqueData = 0;
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
        final Buffer buffer = memoryManager.allocate(FRAME_HEADER_SIZE + 8);

        serializeFrameHeader(buffer);
        buffer.putLong(opaqueData);
        buffer.trim();

        return buffer;
    }

    // ---------------------------------------------------------- Nested Classes

    public static class PingFrameBuilder extends Http2FrameBuilder<PingFrameBuilder> {

        private long opaqueData;

        // -------------------------------------------------------- Constructors

        protected PingFrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods

        public PingFrameBuilder opaqueData(final long opaqueData) {
            this.opaqueData = opaqueData;
            return this;
        }

        public PingFrameBuilder ack(final boolean isAck) {
            if (isAck) {
                setFlag(ACK_FLAG);
            }
            return this;
        }

        @Override
        public PingFrame build() {
            final PingFrame frame = PingFrame.create();
            setHeaderValuesTo(frame);
            frame.opaqueData = opaqueData;

            return frame;
        }

        // --------------------------------------- Methods from Http2FrameBuilder

        @Override
        protected PingFrameBuilder getThis() {
            return this;
        }

    }
}
