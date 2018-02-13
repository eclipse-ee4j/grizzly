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

package org.glassfish.grizzly.utils;

import java.io.IOException;
import java.io.InputStream;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.Buffers;

/**
 * {@link InputStream} implementation over Grizzly {@link Buffer}.
 *
 * @author Alexey Stashok
 */
public class BufferInputStream extends InputStream {

    private final Buffer buffer;

    private final boolean isMovingPosition;
    
    private int position;
    private final int limit;
    
    /**
     * Create the {@link InputStream} over Grizzly {@link Buffer}.
     * Constructed <tt>BufferInputStream</tt> read operations will affect the
     * passed {@link Buffer} position, which means each <tt>BufferInputStream</tt>
     * read operation will shift {@link Buffer}'s position by number of bytes,
     * which were read.
     *
     * @param buffer
     */
    public BufferInputStream(final Buffer buffer) {
        isMovingPosition = true;
        this.buffer = buffer;
        this.position = buffer.position();
        this.limit = buffer.limit();
    }

    /**
     * Create the {@link InputStream} over Grizzly {@link Buffer}.
     * Constructed <tt>BufferInputStream</tt> read operations will *not* affect
     * the passed {@link Buffer} position, which means the passed {@link Buffer}
     * position will never be changed during <tt>BufferInputStream</tt>
     *
     * @param buffer
     */
    public BufferInputStream(final Buffer buffer, final int position,
            final int limit) {
        isMovingPosition = false;
        this.buffer = buffer;
        this.position = position;
        this.limit = limit;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        if (position >= limit) {
            return -1;
        }

        final int result = buffer.get(position++) & 0xFF;

        if (isMovingPosition) {
            buffer.position(position);
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(final byte[] b, final int off, final int len)
            throws IOException {
        if (position >= limit) {
            return -1;
        }

        final int length = Math.min(len, available());

        final int oldPos = buffer.position();
        final int oldLim = buffer.limit();

        if (!isMovingPosition) {
            Buffers.setPositionLimit(buffer, position, limit);
        }
        
        try {
            buffer.get(b, off, length);
        } finally {
            if (!isMovingPosition) {
                Buffers.setPositionLimit(buffer, oldPos, oldLim);
            }
        }

        position += length;

        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return limit - position;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(final long n) throws IOException {
        final int skipped = (int) Math.min(n, available());

        position += skipped;
        if (isMovingPosition) {
            buffer.position(position);
        }
        
        return skipped;
    }
}
