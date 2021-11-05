/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Payara Services Ltd.
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

package org.glassfish.grizzly;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

/**
 * A simple class that abstracts {@link FileChannel#transferTo(long, long, java.nio.channels.WritableByteChannel)} for
 * use with Grizzly 2.0 {@link org.glassfish.grizzly.asyncqueue.AsyncQueueWriter}.
 *
 * @since 2.2
 */
public class FileTransfer implements FileChunk {

    private FileChannel fileChannel;
    private long len;
    private long pos;

    // ------------------------------------------------------------ Constructors

    /**
     * Constructs a new <code>FileTransfer</code> instance backed by the specified {@link File}. This simply calls
     * <code>this(f, 0, f.length)</code>.
     *
     * @param f the {@link File} to transfer.
     *
     * @throws NullPointerException if f is null.
     *
     * @see #FileTransfer(java.io.File, long, long)
     */
    public FileTransfer(final File f) {
        this(f, 0, f.length());
    }

    /**
     * Constructs a new <code>FileTransfer</code> instance backed by the specified {@link File}. The content to transfer
     * will begin at the specified offset, <code>pos</code> with the total transfer length being specified by
     * <code>len</code>.
     *
     * @param f the {@link File} to transfer.
     * @param pos the offset within the File to start the transfer.
     * @param len the total number of bytes to transfer.
     *
     * @throws IllegalArgumentException if <code>f</code> is null, does not exist, is not readable, or is a directory.
     * @throws IllegalArgumentException if <code>pos</code> or <code>len</code> are negative.
     * @throws IllegalArgumentException if len exceeds the number of bytes that may be transferred based on the provided
     * offset and file length.
     */
    public FileTransfer(final File f, final long pos, final long len) {
        if (f == null) {
            throw new IllegalArgumentException("f cannot be null.");
        }
        if (!f.exists()) {
            throw new IllegalArgumentException("File " + f.getAbsolutePath() + " does not exist.");
        }
        if (!f.canRead()) {
            throw new IllegalArgumentException("File " + f.getAbsolutePath() + " is not readable.");
        }
        if (f.isDirectory()) {
            throw new IllegalArgumentException("File " + f.getAbsolutePath() + " is a directory.");
        }
        if (pos < 0) {
            throw new IllegalArgumentException("The pos argument cannot be negative.");
        }
        if (len < 0) {
            throw new IllegalArgumentException("The len argument cannot be negative.");
        }
        if (pos > f.length()) {
            throw new IllegalArgumentException("Illegal offset");
        }
        if (f.length() - pos < len) {
            throw new IllegalArgumentException("Specified length exceeds available bytes to transfer.");
        }

        this.pos = pos;
        this.len = len;
        try {
            fileChannel = new FileInputStream(f).getChannel();
        } catch (FileNotFoundException fnfe) {
            throw new IllegalStateException(fnfe);
        }
    }

    // ---------------------------------------------------------- Public Methods

    /**
     * Transfers the File backing this <code>FileTransfer</code> to the specified {@link WritableByteChannel}.
     *
     * @param c the {@link WritableByteChannel}
     * @return the number of bytes that have been transferred
     * @throws IOException if an error occurs while processing
     * @see java.nio.channels.FileChannel#transferTo(long, long, java.nio.channels.WritableByteChannel)
     */
    public long writeTo(final WritableByteChannel c) throws IOException {
        final long written = fileChannel.transferTo(pos, len, c);
        pos += written;
        len -= written;
        return written;
    }

    // ------------------------------------------ Methods from WritableMessage

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasRemaining() {
        return len != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int remaining() {
        return len > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) len;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean release() {
        try {
            fileChannel.close();
        } catch (IOException ignored) {
        } finally {
            fileChannel = null;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExternal() {
        return true;
    }
}
