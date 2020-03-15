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

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.Transformer;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.utils.CompletionHandlerAdapter;
import org.glassfish.grizzly.utils.ResultAware;
import org.glassfish.grizzly.utils.conditions.Condition;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Each method reads data from the current ByteBuffer.  If not enough data
 * is present in the current ByteBuffer, discard is called on the current
 * ByteBuffer and we advance to the next ByteBuffer, or block if not enough 
 * data is present.  If close() is called, all subsequent method calls will
 * throw an IllegalStateException, and any threads blocked waiting for more
 * data will be unblocked, and continue with an IllegalStateException from the
 * blocking method call.
 * <p>
 * dataReceived and close may be safely invoked by multiple threads.
 * The other methods must be invoked only by one thread, which is the reader of
 * this data stream.
 * 
 * @author Ken Cavanaugh
 * @author Alexey Stashok
 */
public abstract class AbstractStreamReader implements StreamReader {

    private static final boolean DEBUG = false;
    private static final Logger LOGGER = Grizzly.logger(AbstractStreamReader.class);

    protected final Connection connection;

    protected final Input input;

    protected final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    // Concurrency considerations:
    // Only one thread (the consumer) may invoke the readXXX methods.
    // dataReceived and close may be invoked by a producer thread.
    // The consumer thread will invoke readXXX methods far more often
    // than a typical producer will call dataReceived or (possibly) close.
    // So buffers must be protected from concurrent access, either by locking
    // or by a wait-free queue.  However, volatile is sufficient for current,
    // since we just need to ensure the visibility of the value of current to
    // all threads.
    //

    /**
     * Create a new ByteBufferReader.
     *
     * @param connection the {@link Connection} to be associated with this
     *  <code>AbstractStreamReader</code>
     * @param streamInput the stream source
     */
    protected AbstractStreamReader(Connection connection, Input streamInput) {
        this.input = streamInput;
        this.connection = connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean readBoolean() throws IOException {
        return readByte() == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte readByte() throws IOException {
        return input.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char readChar()  throws IOException {
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            if (buffer != null && buffer.remaining() >= 2) {
                final char result = buffer.getChar();
                buffer.shrink();
                return result;
            }
        }

        return (char) ((readByte() & 0xff) << 8 | readByte() & 0xff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short readShort() throws IOException {
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            if (buffer != null && buffer.remaining() >= 2) {
                final short result = buffer.getShort();
                buffer.shrink();
                return result;
            }
        }
        
        return (short) ((readByte() & 0xff) << 8 | readByte() & 0xff);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int readInt() throws IOException {
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            if (buffer != null && buffer.remaining() >= 4) {
                final int result = buffer.getInt();
                buffer.shrink();
                return result;
            }
        }
        
        return (readShort() & 0xffff) << 16 | readShort() & 0xffff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long readLong() throws IOException {
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            if (buffer != null && buffer.remaining() >= 8) {
                final long result = buffer.getLong();
                buffer.shrink();
                return result;
            }
        }
        
        return (readInt() & 0xffffffffL) << 32 | readInt() & 0xffffffffL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public float readFloat() throws IOException {
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            if (buffer != null && buffer.remaining() >= 4) {
                final float result = buffer.getFloat();
                buffer.shrink();
                return result;
            }
        }

        return Float.intBitsToFloat(readInt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    final public double readDouble() throws IOException {
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            if (buffer != null && buffer.remaining() >= 8) {
                final double result = buffer.getDouble();
                buffer.shrink();
                return result;
            }
        }
        
        return Double.longBitsToDouble(readLong());
    }

    private void arraySizeCheck(final int sizeInBytes) {
        if (sizeInBytes > available()) {
            throw new BufferUnderflowException();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBooleanArray(boolean[] data) throws IOException {
        arraySizeCheck(data.length);
        for (int ctr = 0; ctr < data.length; ctr++) {
            data[ctr] = readBoolean();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readByteArray(final byte[] data) throws IOException {
        readByteArray(data, 0, data.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readByteArray(byte[] data, int offset, int length) throws IOException {
        arraySizeCheck(length);
        if (input.isBuffered()) {
            final Buffer buffer = input.getBuffer();
            buffer.get(data, offset, length);
            buffer.shrink();
        } else {
            for(int i = offset; i < length; i++) {
                data[i] = input.read();
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readBytes(final Buffer buffer) throws IOException {
        if (!buffer.hasRemaining()) {
            return;
        }
        
        arraySizeCheck(buffer.remaining());
        if (input.isBuffered()) {
            final Buffer inputBuffer = input.getBuffer();
            final int diff = buffer.remaining() - inputBuffer.remaining();
            if (diff >= 0) {
                buffer.put(inputBuffer);
            } else {
                final int save = inputBuffer.limit();
                inputBuffer.limit(save + diff);
                buffer.put(inputBuffer);
                inputBuffer.limit(save);
            }
            
            inputBuffer.shrink();
        } else {
            while(buffer.hasRemaining()) {
                buffer.put(input.read());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readCharArray(final char[] data) throws IOException {
        arraySizeCheck(2 * data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = readChar();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readShortArray(final short[] data) throws IOException {
        arraySizeCheck(2 * data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = readShort();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readIntArray(final int[] data) throws IOException {
        arraySizeCheck(4 * data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = readInt();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readLongArray(final long[] data) throws IOException {
        arraySizeCheck(8 * data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = readLong();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFloatArray(final float[] data) throws IOException {
        arraySizeCheck(4 * data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = readFloat();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readDoubleArray(final double[] data) throws IOException {
        arraySizeCheck(8 * data.length);
        for (int i = 0; i < data.length; i++) {
            data[i] = readDouble();
        }

    }

    @Override
    public void skip(int length) {
        input.skip(length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E> GrizzlyFuture<E> decode(Transformer<Stream, E> decoder) {
        return decode(decoder, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E> GrizzlyFuture<E> decode(Transformer<Stream, E> decoder, CompletionHandler<E> completionHandler) {
        final FutureImpl<E> future = SafeFutureImpl.create();

        final DecodeCompletionHandler<E, Integer> completionHandlerWrapper =
                new DecodeCompletionHandler<E, Integer>(future, completionHandler);

        notifyCondition(
                new StreamDecodeCondition<E>(this, decoder, completionHandlerWrapper),
                completionHandlerWrapper);
        
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrizzlyFuture<Integer> notifyAvailable(int size) {
        return notifyAvailable(size, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrizzlyFuture<Integer> notifyAvailable(final int size,
            CompletionHandler<Integer> completionHandler) {
        return notifyCondition(new Condition() {
            @Override
            public boolean check() {
                return available() >= size;
            }
        }, completionHandler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrizzlyFuture<Integer> notifyCondition(Condition condition) {
        return notifyCondition(condition, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized GrizzlyFuture<Integer> notifyCondition(
            final Condition condition,
            final CompletionHandler<Integer> completionHandler) {
        if (isClosed()) {
            EOFException exception = new EOFException();
            if (completionHandler != null) {
                completionHandler.failed(exception);
            }

            return ReadyFutureImpl.create(exception);
        }

        return input.notifyCondition(condition, completionHandler);
    }
    
    /**
     * Closes the <tt>StreamReader</tt> and causes all subsequent method calls
     * on this object to throw IllegalStateException.
     */
    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean hasAvailable() {
       return available() > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() {
        return input.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSupportBufferWindow() {
        return input.isBuffered();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer getBufferWindow() {
        return input.getBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Buffer takeBufferWindow() {
        return input.takeBuffer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection() {
        return connection;
    }

    private static class DecodeCompletionHandler<A, B> extends CompletionHandlerAdapter<A, B>
            implements ResultAware<A> {

        private volatile A result;
        
        public DecodeCompletionHandler(FutureImpl<A> future,
                CompletionHandler<A> completionHandler) {
            super(future, completionHandler);
        }
        
        @Override
        public void setResult(A result) {
            this.result = result;
        }

        @Override
        protected A adapt(B result) {
            return this.result;
        }
    }
}

