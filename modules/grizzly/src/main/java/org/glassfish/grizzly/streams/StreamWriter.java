/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.streams;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.Transformer;

/**
 * Write the primitive Java types and arrays of primitives to some data sink.
 * This may include internal buffering for efficiency reasons.
 *
 * Note, that <tt>StreamWriter</tt> implementation may not be thread-safe.
 *
 * @author Ken Cavanaugh
 * @author Alexey Stashok
 */
public interface StreamWriter extends Stream {
    /**
     * Returns <tt>true</tt>, if <tt>StreamReader</tt> has been closed,
     * or <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt>, if <tt>StreamReader</tt> has been closed,
     * or <tt>false</tt> otherwise.
     */
    boolean isClosed();
    
    /**
     * Make sure that all data that has been written is
     * flushed from the stream to its destination.
     */
    GrizzlyFuture<Integer> flush() throws IOException;

    /**
     * Make sure that all data that has been written is
     * flushed from the stream to its destination.
     */
    GrizzlyFuture<Integer> flush(
            CompletionHandler<Integer> completionHandler) throws IOException;

    /**
     * Close the {@link StreamWriter} and make sure all data was flushed.
     */
    GrizzlyFuture<Integer> close(
            CompletionHandler<Integer> completionHandler) throws IOException;

    /**
     * Write the <tt>boolean</tt> value to the <tt>StreamWriter</tt>.
     * 
     * @param data <tt>boolean</tt> value.
     * @throws java.io.IOException
     */
    void writeBoolean(boolean data) throws IOException;

    /**
     * Write the <tt>byte</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>byte</tt> value.
     * @throws java.io.IOException
     */
    void writeByte(byte data) throws IOException;

    /**
     * Write the <tt>char</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>char</tt> value.
     * @throws java.io.IOException
     */
    void writeChar(char data) throws IOException;

    /**
     * Write the <tt>short</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>short</tt> value.
     * @throws java.io.IOException
     */
    void writeShort(short data) throws IOException;

    /**
     * Write the <tt>int</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>int</tt> value.
     * @throws java.io.IOException
     */
    void writeInt(int data) throws IOException;

    /**
     * Write the <tt>long</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>long</tt> value.
     * @throws java.io.IOException
     */
    void writeLong(long data) throws IOException;

    /**
     * Write the <tt>float</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>float</tt> value.
     * @throws java.io.IOException
     */
    void writeFloat(float data) throws IOException;

    /**
     * Write the <tt>double</tt> value to the <tt>StreamWriter</tt>.
     *
     * @param data <tt>double</tt> value.
     * @throws java.io.IOException
     */
    void writeDouble(double data) throws IOException;

    /**
     * Write the array of <tt>boolean</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>boolean</tt> values.
     * @throws java.io.IOException
     */
    void writeBooleanArray(final boolean[] data) throws IOException;

    /**
     * Write the array of <tt>byte</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>byte</tt> values.
     * @throws java.io.IOException
     */
    void writeByteArray(final byte[] data) throws IOException;

    /**
     * Write the part of array of <tt>byte</tt> values to the
     * <tt>StreamWriter</tt>, using specific offset and length values.
     *
     * @param data array of <tt>byte</tt> values.
     * @param offset array offset to start from.
     * @param length number of bytes to write.
     * 
     * @throws java.io.IOException
     */
    void writeByteArray(final byte[] data, int offset, int length)
            throws IOException;

    /**
     * Write the array of <tt>char</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>char</tt> values.
     * @throws java.io.IOException
     */
    void writeCharArray(final char[] data) throws IOException;

    /**
     * Write the array of <tt>short</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>short</tt> values.
     * @throws java.io.IOException
     */
    void writeShortArray(short[] data) throws IOException;

    /**
     * Write the array of <tt>int</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>int</tt> values.
     * @throws java.io.IOException
     */
    void writeIntArray(int[] data) throws IOException;

    /**
     * Write the array of <tt>long</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>long</tt> values.
     * @throws java.io.IOException
     */
    void writeLongArray(long[] data) throws IOException;

    /**
     * Write the array of <tt>float</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>float</tt> values.
     * @throws java.io.IOException
     */
    void writeFloatArray(float[] data) throws IOException;

    /**
     * Write the array of <tt>double</tt> values to the <tt>StreamWriter</tt>.
     *
     * @param data array of <tt>double</tt> values.
     * @throws java.io.IOException
     */
    void writeDoubleArray(double[] data) throws IOException;

    /**
     * Write the {@link Buffer} to the <tt>StreamWriter</tt>.
     *
     * @param buffer {@link Buffer}.
     * 
     * @throws java.io.IOException
     */
    void writeBuffer(Buffer buffer) throws IOException;

    <E> GrizzlyFuture<Stream> encode(Transformer<E, Buffer> encoder,
                                     E object) throws IOException;

    <E> GrizzlyFuture<Stream> encode(Transformer<E, Buffer> encoder,
                                     E object, CompletionHandler<Stream> completionHandler)
            throws IOException;

    /**
     * Get the {@link Connection} this <tt>StreamWriter</tt> belongs to.
     *
     * @return the {@link Connection} this <tt>StreamWriter</tt> belongs to.
     */
    @Override
    Connection getConnection();


    /**
     * Get the timeout for <tt>StreamWriter</tt> I/O operations.
     *
     * @param timeunit timeout unit {@link TimeUnit}.
     * @return the timeout for <tt>StreamWriter</tt> I/O operations.
     */
    long getTimeout(TimeUnit timeunit);

    /**
     * Set the timeout for <tt>StreamWriter</tt> I/O operations.
     *
     * @param timeout the timeout for <tt>StreamWriter</tt> I/O operations.
     * @param timeunit timeout unit {@link TimeUnit}.
     */
    void setTimeout(long timeout, TimeUnit timeunit);
}

