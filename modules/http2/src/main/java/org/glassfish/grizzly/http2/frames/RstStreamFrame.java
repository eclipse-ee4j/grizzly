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

package org.glassfish.grizzly.http2.frames;

import java.util.Collections;
import java.util.Map;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.memory.MemoryManager;

public class RstStreamFrame extends Http2Frame {

    private static final ThreadCache.CachedTypeIndex<RstStreamFrame> CACHE_IDX =
                       ThreadCache.obtainIndex(RstStreamFrame.class, 8);

    public static final int TYPE = 3;

    private ErrorCode errorCode;

    // ------------------------------------------------------------ Constructors


    private RstStreamFrame() { }


    // ---------------------------------------------------------- Public Methods


    static RstStreamFrame create() {
        RstStreamFrame frame = ThreadCache.takeFromCache(CACHE_IDX);
        if (frame == null) {
            frame = new RstStreamFrame();
        }
        return frame;
    }

    public static Http2Frame fromBuffer(final int flags, final int streamId,
            final Buffer frameBuffer) {
        RstStreamFrame frame = create();
        frame.setFlags(flags);
        frame.setStreamId(streamId);
        frame.setFrameBuffer(frameBuffer);
        frame.errorCode = ErrorCode.lookup(frameBuffer.getInt());
        
        return frame;
    }
    
    public static RstStreamFrameBuilder builder() {
        return new RstStreamFrameBuilder();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("RstStreamFrame {")
                .append(headerToString())
                .append(", errorCode=").append(errorCode)
                .append('}');

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

        errorCode = null;
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
        buffer.putInt(errorCode.getCode());
        buffer.trim();
        
        return buffer;
    }


    // ---------------------------------------------------------- Nested Classes
    public static class RstStreamFrameBuilder extends Http2FrameBuilder<RstStreamFrameBuilder> {

        private ErrorCode errorCode;

        // -------------------------------------------------------- Constructors
        protected RstStreamFrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods
        public RstStreamFrameBuilder errorCode(final ErrorCode errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public RstStreamFrame build() {
            final RstStreamFrame frame = RstStreamFrame.create();
            setHeaderValuesTo(frame);
            frame.errorCode = errorCode;

            return frame;
        }

        // --------------------------------------- Methods from Http2FrameBuilder
        @Override
        protected RstStreamFrameBuilder getThis() {
            return this;
        }

    }
}
