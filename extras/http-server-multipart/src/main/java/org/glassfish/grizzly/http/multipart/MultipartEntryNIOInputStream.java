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
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOInputStream;

/**
 * Stream implementation to read {@link MultipartEntry} content in the binary mode.
 *
 * @since 2.0.1
 */
final class MultipartEntryNIOInputStream extends NIOInputStream {

    private boolean isClosed;

//    private final ReadHandler parentReadHandler;
    private final MultipartEntry multipartEntry;
    private NIOInputStream parentNIOInputStream;

    private int requestedSize;
    private ReadHandler handler;
    
    // ------------------------------------------------------------ Constructors


    /**
     * Constructs a new <code>NIOInputStream</code> using the specified
     * {@link #parentNIOInputStream}
     * @param multipartEntry {@link MultipartEntry} the {@link NIOInputStream
     * belongs to.
     */
    public MultipartEntryNIOInputStream(
            final MultipartEntry multipartEntry
            //            final ReadHandler parentReadHandler,
            ) {
        
        this.multipartEntry = multipartEntry;
//        this.parentReadHandler = parentReadHandler;
    }

    /**
     * @param parentNIOInputStream the {@link Request} {@link NIOInputStream}
     * from which binary content will be supplied
     */
    protected void initialize(final NIOInputStream parentNIOInputStream) {
        this.parentNIOInputStream = parentNIOInputStream;
    }
    
    // ------------------------------------------------ Methods from InputStream


    /**
     * {@inheritDoc}
     */
    @Override public int read() throws IOException {
        if (isClosed) {
            throw new IOException();
        }

        if (readyData() == 0) {
            throw new IllegalStateException("Can't be invoked when available() == 0");
        }

        multipartEntry.addAvailableBytes(-1);
//        available--;
        
        return parentNIOInputStream.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        if (isClosed) {
            throw new IOException();
        }

        if (len == 0) {
            return 0;
        }

        final int nlen = Math.min(multipartEntry.availableBytes(), len);
        multipartEntry.addAvailableBytes(-nlen);
//        available -= nlen;
        
        return parentNIOInputStream.read(b, off, nlen);
        
    }

    /**
     * {@inheritDoc}
     */
    @Override public long skip(final long n) throws IOException {
        if (isClosed) {
            throw new IOException();
        }

        if (readyData() < n) {
            throw new IllegalStateException("Can not skip more bytes than available");
        }

        multipartEntry.addAvailableBytes((int) -n);
//        available-= n;

        return parentNIOInputStream.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override public int available() throws IOException {
        return readyData();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void close() throws IOException {
        isClosed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void mark(final int readlimit) {
        parentNIOInputStream.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void reset() throws IOException {
        parentNIOInputStream.reset();
    }

    /**
     * This {@link InputStream} implementation supports marking.
     *
     * @return <code>true</code>
     */
    @Override public boolean markSupported() {
        return parentNIOInputStream.markSupported();
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
                        parentNIOInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }

            return;
        }

        if (shouldNotifyNow(size, multipartEntry.availableBytes())) {
            try {
                handler.onDataAvailable();
            } catch (Exception ioe) {
                try {
                    handler.onError(ioe);
                } finally {
                    try {
                        parentNIOInputStream.close();
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
        return isClosed ? 0 : multipartEntry.availableBytes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReady() {
        return readyData() > 0;
    }


    // --------------------------------------- Methods from BinaryNIOInputSource

    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer getBuffer() {
        final int remaining = readyData();
        final Buffer underlyingBuffer = parentNIOInputStream.getBuffer();
        underlyingBuffer.limit(underlyingBuffer.position() + remaining);
        return underlyingBuffer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer readBuffer() {
        return readBuffer(readyData());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer readBuffer(final int size) {
        if (size > readyData()) {
            throw new IllegalStateException("Can not read more bytes than available");
        }
        multipartEntry.addAvailableBytes(-size);
        return parentNIOInputStream.readBuffer(size);
    }

    
    protected void recycle() {
        parentNIOInputStream = null;
        handler = null;
        isClosed = false;
        requestedSize = 0;
    }

    /**
     * Append available bytes to the input stream
     */
    void onDataCame() throws Exception {
        if (handler == null) return;
        
        try {
            if (isFinished()) {
                handler.onAllDataRead();
            } else if (shouldNotifyNow(requestedSize, multipartEntry.availableBytes())) {
                handler.onDataAvailable();
            }
        } catch (Exception e) {
            try {
                handler.onError(e);
            } finally {
                try {
                    parentNIOInputStream.close();
                } catch (IOException ee) {
                }
            }
            throw e;
        }
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
