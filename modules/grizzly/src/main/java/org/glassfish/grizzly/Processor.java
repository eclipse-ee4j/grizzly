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

package org.glassfish.grizzly;

import org.glassfish.grizzly.asyncqueue.MessageCloner;

/**
 * Processor implementations are responsible for processing I/O events, which
 * occur on connection.
 * 
 * @author Alexey Stashok
 */
@SuppressWarnings("deprecation")
public interface Processor<E extends Context> {
    /**
     * Creates {@link Context}
     *
     * @param connection {@link Connection} to obtain processor for.
     * @return {@link Context}, or <tt>null</tt>, if default {@link Context}
     *         could be used.
     */
    E obtainContext(Connection connection);

    /**
     * Method will be called by framework to process some event, which
     * occurred on a connection
     * 
     * @param context processing context
     * @return the result of I/O event processing
     */
    ProcessorResult process(E context);

    void read(Connection connection,
            CompletionHandler<ReadResult> completionHandler);

    void write(Connection connection,
            Object dstAddress, Object message,
            CompletionHandler<WriteResult> completionHandler);
    
    void write(Connection connection,
            Object dstAddress, Object message,
            CompletionHandler<WriteResult> completionHandler,
            MessageCloner messageCloner);
    
    @Deprecated
    void write(Connection connection,
            Object dstAddress, Object message,
            CompletionHandler<WriteResult> completionHandler,
            org.glassfish.grizzly.asyncqueue.PushBackHandler pushBackHandler);
    
    /**
     * Is this {@link Processor} interested in processing the i/o event
     * 
     * @param ioEvent
     * @return true, if this {@link Processor} is interested and execution
     * process will start, false otherwise.
     */
    boolean isInterested(IOEvent ioEvent);

    /**
     * Set the the i/o event, this {@link Processor} is interested in
     * 
     * @param ioEvent {@link IOEvent}
     * @param isInterested true, if {@link Processor} is interested in
     *                     processing of the I/O event, or false otherwise.
     */
    void setInterested(IOEvent ioEvent, boolean isInterested);
}
