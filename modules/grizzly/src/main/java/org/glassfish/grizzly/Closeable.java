/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * General asynchronous closable interface.
 * 
 * <tt>Closeable</tt> interface contains two sets of methods: close* and terminate*,
 * so interface implementations can provide graceful and abrupt releasing of resources.
 * 
 * @see java.io.Closeable
 * @author Alexey Stashok
 */
public interface Closeable {
    /**
     * Is <tt>Closeable</tt> open and ready.
     * Returns <tt>true</tt>, if the <tt>Closeable</tt> is open and ready, or <tt>false</tt>
     * otherwise.
     * 
     * @return <tt>true</tt>, if <tt>Closeable</tt> is open and ready, or <tt>false</tt>
     * otherwise.
     */
    boolean isOpen();
    
    /**
     * Checks if this <tt>Closeable</tt> is open and ready to be used.
     * If this <tt>Closeable</tt> is closed then an IOException will be thrown
     * @throws IOException giving the reason why this <tt>Closeable</tt>
     * was closed.
     */
    void assertOpen() throws IOException;
    
    /**
     * Closes this stream and releases any system resources associated with it.
     * 
     * If the stream is already closed then invoking this 
     * method has no effect.
     * Use this method, when no completion notification is needed.
     */
    void terminateSilently();

    /**
     * Closes this stream and releases any system resources associated with it.
     * If the stream is already closed then invoking this 
     * method has no effect. 
     *
     * @return {@link java.util.concurrent.Future}, which could be checked in case, if close operation
     *         will be run asynchronously
     */
    GrizzlyFuture<Closeable> terminate();
    
    /**
     * Closes the <tt>Closeable</tt> and provides the reason description.
     * 
     * This method is similar to {@link #terminateSilently()}, but additionally
     * provides the reason why the <tt>Closeable</tt> will be closed.
     * 
     * @param cause reason why terminated. This will be thrown is 
     * {@link #isOpen()} is called subsequently
     */
    void terminateWithReason(IOException cause);
    
    
    /**
     * Gracefully (if supported by the implementation) closes this stream and
     * releases any system resources associated with it.
     * 
     * If the stream is already closed then invoking this 
     * method has no effect.
     * Use this method, when no completion notification is needed.
     */
    void closeSilently();

    /**
     * Gracefully (if supported by the implementation) closes this stream and
     * releases any system resources associated with it.
     * If the stream is already closed then invoking this 
     * method has no effect. 
     *
     * @return {@link java.util.concurrent.Future}, which could be checked in case, if close operation
     *         will be run asynchronously
     * @see java.io.Closeable#close() which is not asynchronous
     */
    GrizzlyFuture<Closeable> close();
    
    /**
     * Gracefully closes this stream and releases any system resources associated
     * with it.
     * This operation waits for all pending output data to be flushed before
     * closing the stream.
     * If the stream is already closed then invoking this 
     * method has no effect. 
     *
     * @param completionHandler {@link CompletionHandler} to be called, when
     *  the stream is closed
     * @deprecated please use {@link #close()} with the following {@link
     *  GrizzlyFuture#addCompletionHandler(org.glassfish.grizzly.CompletionHandler)} call
     */
    void close(CompletionHandler<Closeable> completionHandler);
    
    /**
     * Gracefully closes the <tt>Closeable</tt> and provides the reason description.
     * 
     * This method is similar to {@link #closeSilently()}, but additionally
     * provides the reason why the <tt>Closeable</tt> will be closed.
     * 
     * @param cause reason why closed, this will be thrown by
     * {@link #isOpen()} if called subsequently
     */
    void closeWithReason(IOException cause);
    
    /**
     * Add the {@link CloseListener}, which will be notified once the stream
     * will be closed.
     * 
     * @param closeListener {@link CloseListener}.
     */
    void addCloseListener(CloseListener closeListener);

    /**
     * Remove the {@link CloseListener}.
     *
     * @param closeListener {@link CloseListener}.
     * @return <tt>true</tt> if the listener was successfully removed, or
     *         <tt>false</tt> otherwise.
     */
    boolean removeCloseListener(CloseListener closeListener);
    
    /**
     * @return the {@link Future}, that will be notified once this <tt>Closeable</tt>
     *          is closed
     * @since 2.3.24
     */
    GrizzlyFuture<CloseReason> closeFuture();
}
