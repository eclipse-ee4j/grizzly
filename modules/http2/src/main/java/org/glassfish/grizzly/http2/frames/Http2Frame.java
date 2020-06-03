/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.memory.MemoryManager;

public abstract class Http2Frame implements Cacheable {

    public static final int FRAME_HEADER_SIZE = 9;

    protected static final boolean DONT_RECYCLE = Boolean.getBoolean(Http2Frame.class.getName() + ".dont-recycle");

    // these are common to all frames
    private int flags;
    private int streamId = 0;
    protected int length = -1;

    protected Buffer frameBuffer;

    // ------------------------------------------------------------ Constructors

    protected Http2Frame() {
    }

    // ---------------------------------------------------------- Public Methods

    public Buffer toBuffer() {
        return toBuffer(MemoryManager.DEFAULT_MEMORY_MANAGER);
    }

    public abstract Buffer toBuffer(final MemoryManager memoryManager);

    public boolean isFlagSet(final int flag) {
        return (flags & flag) == flag;
    }

    public void setFlag(final int flag) {
        flags |= flag;
    }

    public void clearFlag(final int flag) {
        flags &= ~flag;
    }

    /**
     * @return flags for the frame. Only the first 8 bits are relevant.
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Sets flags for the frame. Only the first 8 bits are relevant.
     * 
     * @param flags the flags for this frame
     */
    protected void setFlags(final int flags) {
        this.flags = flags;
    }

    /**
     * @return the length of this frame.
     */
    public int getLength() {
        if (length == -1) {
            length = calcLength();
        }

        return length;
    }

    /**
     * @return the length of this frame.
     */
    protected abstract int calcLength();

    /**
     * @return the {@link Map} with flag bit - to - flag name mapping
     */
    protected abstract Map<Integer, String> getFlagNamesMap();

    /**
     * The method should be invoked once packet payload is updated
     */
    protected void onPayloadUpdated() {
        length = -1;
    }

    /**
     * @return the type of the frame.
     */
    public abstract int getType();

    /**
     * @return the stream ID associated with the frame.
     */
    public int getStreamId() {
        return streamId;
    }

    /**
     * Sets the stream ID associated with the data frame.
     * 
     * @param streamId the stream ID of this frame.
     */
    protected void setStreamId(final int streamId) {
        this.streamId = streamId;
    }

    @Override
    public String toString() {
        return '{' + headerToString() + '}';
    }

    public String headerToString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("streamId=").append(streamId);
        sb.append(", type=").append(getType());
        sb.append(", flags=[").append(flagsToString(flags, getFlagNamesMap())).append(']');
        sb.append(", length=").append(getLength());
        return sb.toString();
    }

    protected void setFrameBuffer(Buffer frameBuffer) {
        this.frameBuffer = frameBuffer;
    }

    protected void serializeFrameHeader(final Buffer buffer) {
        assert buffer.remaining() >= Http2Frame.FRAME_HEADER_SIZE;

        buffer.putInt((getLength() & 0xffffff) << 8 | getType());
        buffer.put((byte) getFlags());
        buffer.putInt(getStreamId());
    }

    // -------------------------------------------------- Methods from Cacheable

    @Override
    public void recycle() {
        if (DONT_RECYCLE) {
            return;
        }

        flags = 0;
        length = -1;
        streamId = 0;

        if (frameBuffer != null) {
            frameBuffer.tryDispose();
            frameBuffer = null;
        }
    }

    private static String flagsToString(int flags, final Map<Integer, String> flagsNameMap) {
        if (flags == 0) {
            return "none";
        }

        final StringBuilder sb = new StringBuilder();
        while (flags != 0) {
            final int flagsNext = flags & flags - 1; // flags without lowest 1

            final int lowestOneBit = flags - flagsNext;
            final String name = flagsNameMap.get(lowestOneBit);

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append(name != null ? name : sb.append('#').append(Integer.numberOfLeadingZeros(flags)));

            flags = flagsNext;
        }

        return sb.toString();
    }
    // ---------------------------------------------------------- Nested Classes

    protected static abstract class Http2FrameBuilder<T extends Http2FrameBuilder> {
        protected int flags;
        protected int streamId;

        // -------------------------------------------------------- Constructors

        protected Http2FrameBuilder() {
        }

        // ------------------------------------------------------ Public Methods
        public abstract Http2Frame build();

        public T setFlag(final int flag) {
            this.flags |= flag;
            return getThis();
        }

        @SuppressWarnings("unused")
        public T clearFlag(final int flag) {
            flags &= ~flag;
            return getThis();
        }

        public T streamId(final int streamId) {
            this.streamId = streamId;
            return getThis();
        }

        protected void setHeaderValuesTo(final Http2Frame frame) {
            frame.flags = flags;
            frame.streamId = streamId;
        }

        // --------------------------------------------------- Protected Methods

        protected abstract T getThis();

    }
}
