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

package org.glassfish.grizzly.http2;

import java.util.ArrayList;
import java.util.List;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.http2.frames.Http2Frame;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * The {@link Filter} responsible for transforming {@link Http2Frame}s to {@link Buffer}s and vise versa.
 *
 * @author Grizzly team
 */
public class Http2FrameCodec {

    /**
     *
     * @param http2Session the {@link Http2Session} from which the source buffer was obtained.
     * @param parsingState the current {@link FrameParsingState}.
     * @param srcMessage the inbound buffer.
     * @return one or more {@link Http2Frame}s parsed from the source message.
     *
     * @throws Http2SessionException if an error occurs parsing the frame(s).
     */
    public List<Http2Frame> parse(final Http2Session http2Session, final FrameParsingState parsingState, Buffer srcMessage) throws Http2SessionException {

        if (parsingState.bytesToSkip() > 0) {
            if (!skip(parsingState, srcMessage)) {
                return null;
            }
        }

        srcMessage = parsingState.appendToRemainder(http2Session.getMemoryManager(), srcMessage);

        ParsingResult parsingResult = parseFrame(http2Session, parsingState, srcMessage);

        if (!parsingResult.isReady()) {
            return null;
        }

        Buffer remainder = parsingResult.remainder();

        while (remainder.remaining() >= Http2Frame.FRAME_HEADER_SIZE) {
            parsingResult = parseFrame(http2Session, parsingState, remainder);

            if (!parsingResult.isReady()) {
                return parsingResult.frameList();
            }

            remainder = parsingResult.remainder();

        }

        return parsingResult.frameList();
    }

    public Buffer serializeAndRecycle(final Http2Session http2Session, final Http2Frame frame) {

        NetLogger.log(NetLogger.Context.TX, http2Session, frame);

        final Buffer resultBuffer = frame.toBuffer(http2Session.getMemoryManager());
        frame.recycle();
        return resultBuffer;
    }

    public Buffer serializeAndRecycle(final Http2Session http2Session, final List<Http2Frame> frames) {

        Buffer resultBuffer = null;

        final int framesCount = frames.size();

        for (int i = 0; i < framesCount; i++) {
            final Http2Frame frame = frames.get(i);
            NetLogger.log(NetLogger.Context.TX, http2Session, frame);
            final Buffer buffer = frame.toBuffer(http2Session.getMemoryManager());
            frame.recycle();

            resultBuffer = Buffers.appendBuffers(http2Session.getMemoryManager(), resultBuffer, buffer);
        }

        frames.clear();

        return resultBuffer;
    }

    // --------------------------------------------------------- Private Methods

    private ParsingResult parseFrame(final Http2Session http2Session, final FrameParsingState state, final Buffer buffer) throws Http2SessionException {

        final int bufferSize = buffer.remaining();
        final ParsingResult parsingResult = state.parsingResult();

        if (bufferSize < Http2Frame.FRAME_HEADER_SIZE) {
            return parsingResult.setNeedMore(buffer);
        }

        final int len = http2Session.getFrameSize(buffer);

        if (len > http2Session.getLocalMaxFramePayloadSize() + Http2Frame.FRAME_HEADER_SIZE) {

            http2Session.onOversizedFrame(buffer);

            // skip the frame header
            buffer.position(buffer.position() + Http2Frame.FRAME_HEADER_SIZE);

            // figure out what to do with the remainder
            final Buffer remainder;
            final int remaining = buffer.remaining();

            if (remaining > len) {
                final int bufferPos = buffer.position();
                remainder = buffer.split(bufferPos + len);
            } else {
                remainder = Buffers.EMPTY_BUFFER;
                state.bytesToSkip(len - remaining);
            }

            return parsingResult.setParsed(null, remainder);
        }

        if (buffer.remaining() < len) {
            return parsingResult.setNeedMore(buffer);
        }

        final Buffer remainder = buffer.split(buffer.position() + len);
        final Http2Frame frame = http2Session.parseHttp2FrameHeader(buffer);

        return parsingResult.setParsed(frame, remainder);
    }

    private boolean skip(final FrameParsingState parsingState, final Buffer message) {

        final int bytesToSkip = parsingState.bytesToSkip();

        final int dec = Math.min(bytesToSkip, message.remaining());
        parsingState.bytesToSkip(bytesToSkip - dec);

        message.position(message.position() + dec);

        if (message.hasRemaining()) {
            message.shrink();
            return true;
        }

        message.tryDispose();
        return false;
    }

    public final static class FrameParsingState {
        private int bytesToSkip;
        private final ParsingResult parsingResult = new ParsingResult();

        List<Http2Frame> getList() {
            return parsingResult.frameList;
        }

        Buffer appendToRemainder(final MemoryManager mm, final Buffer buffer) {
            final Buffer remainderBuffer = parsingResult.remainder;
            parsingResult.remainder = null;
            return Buffers.appendBuffers(mm, remainderBuffer, buffer, true);
        }

        int bytesToSkip() {
            return bytesToSkip;
        }

        void bytesToSkip(final int bytesToSkip) {
            this.bytesToSkip = bytesToSkip;
        }

        ParsingResult parsingResult() {
            return parsingResult;
        }
    }

    final static class ParsingResult {
        private Buffer remainder;
        private boolean isReady;
        private final List<Http2Frame> frameList = new ArrayList<>(4);

        private ParsingResult() {
        }

        ParsingResult setParsed(final Http2Frame frame, final Buffer remainder) {
            if (frame != null) {
                frameList.add(frame);
            }

            this.remainder = remainder;
            isReady = true;

            return this;
        }

        ParsingResult setNeedMore(final Buffer remainder) {
            this.remainder = remainder;
            isReady = false;

            return this;
        }

        List<Http2Frame> frameList() {
            return frameList;
        }

        Buffer remainder() {
            return remainder;
        }

        boolean isReady() {
            return isReady;
        }
    }
}
