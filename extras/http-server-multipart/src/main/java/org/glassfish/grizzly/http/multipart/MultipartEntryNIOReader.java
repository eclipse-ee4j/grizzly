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

package org.glassfish.grizzly.http.multipart;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOInputStream;
import org.glassfish.grizzly.http.io.NIOReader;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Stream implementation to read {@link MultipartEntry} content in the character mode.
 *
 * @since 2.0.1
 */

final class MultipartEntryNIOReader extends NIOReader {

    private boolean isClosed;

//    private final ReadHandler parentReadHandler;
    private final MultipartEntry multipartEntry;
    private NIOInputStream requestNIOInputStream;

    private String encoding;
    private CharsetDecoder decoder;
    private float averageCharsPerByte;
    /**
     * {@link CharBuffer} for converting a single character at a time.
     */
    private final CharBuffer singleCharBuf = CharBuffer.allocate(1);

    private int requestedSize;
    private ReadHandler handler;

    // ------------------------------------------------------------ Constructors
    
    /**
     * Constructs a new <code>NIOInputStream</code> using the specified
     * {@link #requestNIOInputStream}
     * @param multipartEntry {@link MultipartEntry} the {@link NIOInputStream
     * belongs to.
     */
    public MultipartEntryNIOReader(final MultipartEntry multipartEntry) {
        this.multipartEntry = multipartEntry;
    }

    /**
     * 
     * @param requestNIOInputStream the {@link Request} {@link NIOInputStream}
     * from which binary content will be supplied
     */
    protected void initialize(final NIOInputStream requestInputStream,
            final String encoding) {
        this.requestNIOInputStream = requestInputStream;
        this.encoding = encoding;
        this.averageCharsPerByte = getDecoder().averageCharsPerByte();
    }
    // ----------------------------------------------------- Methods from Reader


    /**
     * {@inheritDoc}
     */
    @Override public int read(final CharBuffer target) throws IOException {
        final int count = fillChar(target.capacity(), target);
        target.flip();
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int read() throws IOException {
        if (isClosed) {
            throw new IOException();
        }

        singleCharBuf.position(0);
        int read = read(singleCharBuf);
        if (read == -1) {
            return -1;
        }
        final char c = singleCharBuf.get(0);

        singleCharBuf.position(0);
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override public int read(final char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override public int read(final char[] cbuf, final int off, final int len)
    throws IOException {
        if (isClosed) {
            throw new IOException();
        }

        if (len == 0) {
            return 0;
        }

        final CharBuffer buf = CharBuffer.wrap(cbuf, off, len);
        return read(buf);
    }

    /**
     * {@inheritDoc}
     */
    @Override public long skip(final long n) throws IOException {
        if (isClosed) {
            throw new IOException();
        }


        if (n > multipartEntry.availableBytes()) {
            throw new IllegalStateException("Can not skip more bytes than available");
        }

        if (n < 0) { // required by java.io.Reader.skip()
            throw new IllegalArgumentException();
        }
        if (n == 0) {
            return 0L;
        }
        
        final CharBuffer skipBuffer = CharBuffer.allocate((int) n);

        if (fillChar((int) n, skipBuffer) == -1) {
            return 0;
        }
        return Math.min(skipBuffer.position(), n);
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean ready() throws IOException {
        return isReady();
    }

    /**
     * This {@link Reader} implementation does not support marking.
     *
     * @return <code>false</code>
     */
    @Override public boolean markSupported() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void mark(int readAheadLimit) throws IOException {
        throw new IOException();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void reset() throws IOException {
        throw new IOException();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void close() throws IOException {
        isClosed = true;
    }

    // --------------------------------------------- Methods from InputSource

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyAvailable(final ReadHandler handler) {
        notifyAvailable(handler, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void notifyAvailable(final ReadHandler handler, final int size) {
        // If we don't expect more data - call onAllDataRead() directly
        if (isClosed || isFinished()) {
            try {
                handler.onAllDataRead();
            } catch (Exception ioe) {
                try {
                    handler.onError(ioe);
                } finally {
                    try {
                        requestNIOInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }

            return;
        }

        if (shouldNotifyNow(size, readyData())) {
            try {
                handler.onDataAvailable();
            } catch (Exception ioe) {
                try {
                    handler.onError(ioe);
                } finally {
                    try {
                        requestNIOInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
            return;
        }

        requestedSize = size;
        this.handler = handler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFinished() {
        return multipartEntry.isFinished();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readyData() {
        return ((int) (multipartEntry.availableBytes() * averageCharsPerByte));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        return readyData() > 0;
    }

    protected void recycle() {
        requestNIOInputStream = null;
        decoder = null;
        averageCharsPerByte = 1;
        encoding = null;
        handler = null;
        isClosed = false;
        requestedSize = 0;
    }

    /**
     * Append available bytes to the input stream
     */
    void onDataCame() {
        if (handler == null) return;

        try {
            if (isFinished()) {
                handler.onAllDataRead();
            } else if (shouldNotifyNow(requestedSize, readyData())) {
                handler.onDataAvailable();
            }
        } catch (Exception e) {
            try {
                handler.onError(e);
            } finally {
                try {
                    requestNIOInputStream.close();
                } catch (IOException ee) {
                }
            }
        }
    }
    /**
     * <p>
     * Used to convert bytes to char.
     * </p>
     *
     * @param requestedLen how much content should attempt to be read
     *
     * @return the number of bytes actually read
     *
     * @throws IOException if an I/O error occurs while reading content
     */
    private int fillChar(final int requestedLen,
                         final CharBuffer dst) throws IOException {

        final int charPos = dst.position();
        final ByteBuffer bb = requestNIOInputStream.getBuffer().toByteBuffer();
        final int bbPos = bb.position();
        final int bbLim = bb.limit();
        bb.limit(bbPos + multipartEntry.availableBytes());
        getDecoder().decode(bb, dst, false);

        int readChars = dst.position() - charPos;
        int readBytes = bb.position() - bbPos;
        bb.position(bbPos);
        bb.limit(bbLim);

        requestNIOInputStream.skip(readBytes);
        multipartEntry.addAvailableBytes(-readBytes);

        if (multipartEntry.availableBytes() > 0 && readChars < requestedLen) {
            readChars += fillChar(0, dst);
        }

        return readChars;
    }

    /**
     * @return the {@link CharsetDecoder} that should be used when converting
     *  content from binary to character
     */
    private CharsetDecoder getDecoder() {

        if (decoder == null) {
            Charset cs = Charsets.lookupCharset(encoding);
            decoder = cs.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPLACE);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
        }

        return decoder;

    }
    
    /**
     * @param size the amount of data that must be available for a {@link ReadHandler}
     *  to be notified.
     * @param available the amount of data currently available.
     *
     * @return <code>true</code> if the handler should be notified during a call
     *  to {@link #notifyAvailable(ReadHandler)} or {@link #notifyAvailable(ReadHandler, int)},
     *  otherwise <code>false</code>
     */
    private static boolean shouldNotifyNow(final int size, final int available) {

        return available != 0 && available >= size;

    }
}
