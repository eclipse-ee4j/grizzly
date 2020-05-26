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
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

public class DataFrame extends Http2Frame {

    private static final ThreadCache.CachedTypeIndex<DataFrame> CACHE_IDX = ThreadCache.obtainIndex(DataFrame.class, 8);

    public static final int TYPE = 0;

    public static final byte END_STREAM = 0x1;
    public static final byte PADDED = 0x8;

    static final Map<Integer, String> FLAG_NAMES_MAP = new HashMap<>(4);

    static {
        FLAG_NAMES_MAP.put((int) END_STREAM, "END_STREAM");
        FLAG_NAMES_MAP.put((int) PADDED, "PADDED");
    }

    private Buffer data;
    private int padLength;

    // ------------------------------------------------------------ Constructors

    private DataFrame() {
    }

    // ---------------------------------------------------------- Public Methods

    static DataFrame create() {
        DataFrame frame = ThreadCache.takeFromCache(CACHE_IDX);
        if (frame == null) {
            frame = new DataFrame();
        }
        return frame;
    }

    public static DataFrame fromBuffer(final int flags, final int streamId, final Buffer buffer) {
        final DataFrame frame = create();
        frame.setFlags(flags);
        frame.setStreamId(streamId);

        if (frame.isFlagSet(PADDED)) {
            frame.padLength = buffer.get() & 0xFF;
        }

        // split the Buffer so data Buffer won't be disposed on frame recycle
        frame.data = buffer.split(buffer.position());
        frame.setFrameBuffer(buffer);

        frame.onPayloadUpdated();

        return frame;
    }

    public static DataFrameBuilder builder() {
        return new DataFrameBuilder();
    }

    /**
     * Remove DataFrame padding (if it was applied).
     *
     * @return this DataFrame instance
     */
    public DataFrame normalize() {
        if (isPadded()) {
            clearFlag(PADDED);
            data.limit(data.limit() - padLength);
            padLength = 0;

            onPayloadUpdated();
        }

        return this;
    }

    public Buffer getData() {
        return data;
    }

    public int getPadLength() {
        return padLength;
    }

    public boolean isEndStream() {
        return isFlagSet(END_STREAM);
    }

    public boolean isPadded() {
        return isFlagSet(PADDED);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("DataFrame {").append(headerToString()).append(", data=").append(data).append('}');
        return sb.toString();
    }

    @Override
    protected int calcLength() {
        return data.remaining() + (isPadded() ? 1 : 0);
    }

    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        if (DONT_RECYCLE) {
            return;
        }

        padLength = 0;
        data = null;

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

        final boolean isPadded = isFlagSet(PADDED);
        final int extraHeaderLen = isPadded ? 1 : 0;
        final Buffer buffer = memoryManager.allocate(FRAME_HEADER_SIZE + extraHeaderLen);

        serializeFrameHeader(buffer);

        if (isPadded) {
            buffer.put((byte) (padLength & 0xff));
        }

        buffer.trim();
        final CompositeBuffer cb = CompositeBuffer.newBuffer(memoryManager, buffer, data);

        cb.allowBufferDispose(true);
        cb.allowInternalBuffersDispose(true);

        return cb;
    }

    @Override
    protected Map<Integer, String> getFlagNamesMap() {
        return FLAG_NAMES_MAP;
    }
    // ---------------------------------------------------------- Nested Classes

    public static class DataFrameBuilder extends Http2FrameBuilder<DataFrameBuilder> {

        private Buffer data;
        private int padLength;

        // -------------------------------------------------------- Constructors

        protected DataFrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods

        public DataFrameBuilder data(final Buffer data) {
            this.data = data;

            return this;
        }

        public DataFrameBuilder endStream(boolean endStream) {
            if (endStream) {
                setFlag(HeadersFrame.END_STREAM);
            }
            return this;
        }

        public DataFrameBuilder padded(boolean isPadded) {
            if (isPadded) {
                setFlag(HeadersFrame.PADDED);
            }
            return this;
        }

        public void padLength(int padLength) {
            this.padLength = padLength;
        }

        @Override
        public DataFrame build() {
            final DataFrame frame = DataFrame.create();
            setHeaderValuesTo(frame);
            frame.data = data;
            frame.padLength = padLength;

            return frame;
        }

        // --------------------------------------- Methods from Http2FrameBuilder

        @Override
        protected DataFrameBuilder getThis() {
            return this;
        }

    } // END DataFrameBuilder

}
