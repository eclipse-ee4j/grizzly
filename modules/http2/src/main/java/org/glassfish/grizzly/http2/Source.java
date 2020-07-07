/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http2.frames.ErrorCode;
import org.glassfish.grizzly.memory.Buffers;

/**
 * The class represents generic source of data to be sent on {@link Http2Stream}.
 *
 * @author Alexey Stashok
 */
abstract class Source {
    /**
     * Returns the number of bytes remaining to be written.
     */
    public abstract int remaining();

    /**
     * Returns the number of bytes to be written.
     * 
     * @param length max number of bytes to return.
     * @return {@link Buffer}, which contains up to <tt>length</tt> bytes (could return less) to be written. <tt>null</tt>
     * result is not permitted.
     *
     * @throws Http2StreamException if an error occurs reading from the stream.
     */
    public abstract Buffer read(final int length) throws Http2StreamException;

    /**
     * Returns <tt>true</tt> if there is more data to be written, or <tt>false</tt> otherwise.
     */
    public abstract boolean hasRemaining();

    /**
     * The method is called, when the source might be released/closed.
     */
    public abstract void release();

    /**
     * Returns the {@link SourceFactory} associated with the {@link Http2Stream}.
     */
    public static SourceFactory factory(final Http2Stream http2Stream) {
        return new SourceFactory(http2Stream);
    }

    /**
     * The helper factory class to create {@link Source}s based on {@link File}, {@link Buffer}, {@link String} and byte[].
     */
    public final static class SourceFactory {

        private final Http2Stream stream;

        private SourceFactory(final Http2Stream stream) {
            this.stream = stream;
        }

        /**
         * Create {@link Source} based on {@link Buffer}.
         *
         * @param buffer {@link Buffer} to be written.
         *
         * @return {@link Source}.
         */
        public Source createBufferSource(final Buffer buffer) {
            return new BufferSource(buffer, stream);
        }

        /**
         * {@link Source} implementation based on {@link Buffer}.
         */
        private static class BufferSource extends Source {
            private boolean isClosed;

            private Buffer buffer;

            private final Http2Stream stream;

            protected BufferSource(final Buffer buffer, final Http2Stream stream) {

                this.buffer = buffer;
                this.stream = stream;
            }

            @Override
            public int remaining() {
                return buffer.remaining();
            }

            @Override
            public Buffer read(final int length) throws Http2StreamException {
                if (isClosed) {
                    throw new Http2StreamException(stream.getId(), ErrorCode.INTERNAL_ERROR, "The source was closed");
                }

                final int remaining = buffer.remaining();
                if (length == 0 || remaining == 0) {
                    return Buffers.EMPTY_BUFFER;
                }

                final int bytesToSplit = Math.min(remaining, length);
                final Buffer newBuf = buffer.split(buffer.position() + bytesToSplit);
                final Buffer bufferToReturn = buffer;
                buffer = newBuf;

                return bufferToReturn;
            }

            @Override
            public boolean hasRemaining() {
                return !isClosed && buffer.hasRemaining();
            }

            @Override
            public void release() {
                if (!isClosed) {
                    isClosed = true;
                    buffer.tryDispose();
                }
            }
        }
    }
}
