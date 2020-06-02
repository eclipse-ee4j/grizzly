/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.concurrent.Future;

import org.glassfish.grizzly.asyncqueue.MessageCloner;
import org.glassfish.grizzly.asyncqueue.WritableMessage;

/**
 * Implementations of this interface are able to write data from a {@link Buffer} to {@link Connection}.
 *
 * There are two basic Writer implementations in Grizzly: {@link org.glassfish.grizzly.asyncqueue.AsyncQueueWriter},
 * {@link org.glassfish.grizzly.nio.tmpselectors.TemporarySelectorWriter}.
 *
 * @param <L> the writer address type
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("deprecation")
public interface Writer<L> {
    /**
     * Method writes the {@link WritableMessage}.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param message the {@link WritableMessage}, from which the data will be written
     * @return {@link Future}, using which it's possible to check the result
     * @throws java.io.IOException not thrown
     */
    GrizzlyFuture<WriteResult<WritableMessage, L>> write(Connection<L> connection, WritableMessage message) throws IOException;

    /**
     * Method writes the {@link WritableMessage}.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param message the {@link WritableMessage}, from which the data will be written
     * @param completionHandler {@link org.glassfish.grizzly.CompletionHandler}, which will get notified, when write will be
     * completed
     */
    void write(Connection<L> connection, WritableMessage message, CompletionHandler<WriteResult<WritableMessage, L>> completionHandler);

    /**
     * Method writes the {@link WritableMessage} to the specific address.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param dstAddress the destination address the {@link WritableMessage} will be sent to
     * @param message the {@link WritableMessage}, from which the data will be written
     * @return {@link Future}, using which it's possible to check the result
     */
    GrizzlyFuture<WriteResult<WritableMessage, L>> write(Connection<L> connection, L dstAddress, WritableMessage message);

    /**
     * Method writes the {@link WritableMessage} to the specific address.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param dstAddress the destination address the {@link WritableMessage} will be sent to
     * @param message the {@link WritableMessage}, from which the data will be written
     * @param completionHandler {@link org.glassfish.grizzly.CompletionHandler}, which will get notified, when write will be
     * completed
     */
    void write(Connection<L> connection, L dstAddress, WritableMessage message, CompletionHandler<WriteResult<WritableMessage, L>> completionHandler);

    /**
     * Method writes the {@link WritableMessage} to the specific address.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param dstAddress the destination address the {@link WritableMessage} will be sent to
     * @param message the {@link WritableMessage}, from which the data will be written
     * @param completionHandler {@link org.glassfish.grizzly.CompletionHandler}, which will get notified, when write will be
     * completed
     * @param pushBackHandler {@link org.glassfish.grizzly.asyncqueue.PushBackHandler}, which will be notified if message
     * was accepted by transport write queue or refused
     * @deprecated push back logic is deprecated
     */
    @Deprecated
    void write(Connection<L> connection, L dstAddress, WritableMessage message, CompletionHandler<WriteResult<WritableMessage, L>> completionHandler,
            org.glassfish.grizzly.asyncqueue.PushBackHandler pushBackHandler);

    /**
     * Method writes the {@link WritableMessage} to the specific address.
     *
     *
     * @param connection the {@link org.glassfish.grizzly.Connection} to write to
     * @param dstAddress the destination address the {@link WritableMessage} will be sent to
     * @param message the {@link WritableMessage}, from which the data will be written
     * @param completionHandler {@link org.glassfish.grizzly.CompletionHandler}, which will get notified, when write will be
     * completed
     * @param messageCloner the {@link MessageCloner}, which will be able to clone the message in case it can't be
     * completely written in the current thread.
     */
    void write(Connection<L> connection, L dstAddress, WritableMessage message, CompletionHandler<WriteResult<WritableMessage, L>> completionHandler,
            MessageCloner<WritableMessage> messageCloner);

    /**
     * Return <code>true</code> if the connection has not exceeded it's maximum size in bytes of pending writes, otherwise
     * <code>false</code>.
     *
     * @param connection the {@link Connection} to test whether or not the specified number of bytes can be written to.
     * @return <code>true</code> if the connection has not exceeded it's maximum size in bytes of pending writes, otherwise
     * <code>false</code>
     *
     * @since 2.3
     */
    boolean canWrite(final Connection<L> connection);

    /**
     * Registers {@link WriteHandler}, which will be notified ones at least one byte can be written.
     *
     * This method call is equivalent to call notifyWritePossible(connection, writeHandler, <tt>1</tt>);
     *
     * Note: using this method from different threads simultaneously may lead to quick situation changes, so at time
     * {@link WriteHandler} is called - the queue may become busy again.
     *
     * @param connection {@link Connection}
     * @param writeHandler {@link WriteHandler} to be notified.
     *
     * @since 2.3
     */
    void notifyWritePossible(final Connection<L> connection, final WriteHandler writeHandler);

    /**
     * Write reentrants counter
     */
    final class Reentrant {
        private static final ThreadLocal<Reentrant> REENTRANTS_COUNTER = new ThreadLocal<Reentrant>() {

            @Override
            protected Reentrant initialValue() {
                return new Reentrant();
            }
        };

        private static final int maxWriteReentrants = Integer.getInteger("org.glassfish.grizzly.Writer.max-write-reentrants", 10);

        /**
         * Returns the maximum number of write() method reentrants a thread is allowed to made. This is related to possible
         * write()-&gt;onComplete()-&gt;write()-&gt;... chain, which may grow infinitely and cause StackOverflow. Using
         * maxWriteReentrants value it's possible to limit such a chain.
         *
         * @return the maximum number of write() method reentrants a thread is allowed to make.
         */
        public static int getMaxReentrants() {
            return maxWriteReentrants;
        }

        /**
         * Returns the current write reentrants counter. Might be useful, if developer wants to use custom notification
         * mechanism, based on on {@link #canWrite(org.glassfish.grizzly.Connection)} and various write methods.
         * 
         * @return current reentrants counter
         */
        public static Reentrant getWriteReentrant() {
            // ThreadLocal otherwise
            return REENTRANTS_COUNTER.get();
        }

        private int counter;

        /**
         * Returns the value of the reentrants counter for the current thread.
         */
        public int get() {
            return counter;
        }

        /**
         * Increments the reentrants counter by one.
         *
         * @return <tt>true</tt> if the counter (after incrementing) didn't reach {@link #getMaxReentrants()} limit, or
         * <tt>false</tt> otherwise.
         */
        public boolean inc() {
            return ++counter <= maxWriteReentrants;
        }

        /**
         * Decrements the reentrants counter by one.
         *
         * @return <tt>true</tt> if the counter (after decrementing) didn't reach {@link #getMaxReentrants()} limit, or
         * <tt>false</tt> otherwise.
         */
        public boolean dec() {
            return --counter <= maxWriteReentrants;
        }

        /**
         * Returns <tt>true</tt>, if max number of write->completion-handler reentrants has been reached for the passed
         * {@link Reentrant} object, and next write will happen in the separate thread.
         *
         * @return <tt>true</tt>, if max number of write->completion-handler reentrants has been reached for the passed
         * {@link Reentrant} object, and next write will happen in the separate thread.
         */
        public boolean isMaxReentrantsReached() {
            return get() >= getMaxReentrants();
        }
    }
}
