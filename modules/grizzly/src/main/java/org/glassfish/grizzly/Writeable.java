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

import java.util.concurrent.Future;

/**
 * Implementations of this interface are able to write data from a {@link Buffer}.
 *
 * Grizzly {@link Connection} extends {@link Writeable}.
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("deprecation")
public interface Writeable<L> extends OutputSink {

    /**
     * Method writes the <tt>buffer</tt>.
     *
     * @param message the buffer, from which the data will be written
     * @return {@link Future}, using which it's possible to check the
     *         result
     */
     <M> GrizzlyFuture<WriteResult<M, L>> write(M message);

    /**
     * Method writes the <tt>buffer</tt>.
     *
     * @param message the buffer, from which the data will be written
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when write will be completed
     */
     <M> void write(M message,
            CompletionHandler<WriteResult<M, L>> completionHandler);

    /**
     * Method writes the <tt>buffer</tt>.
     *
     * @param message the buffer, from which the data will be written
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when write will be completed
     * @param pushbackHandler {@link org.glassfish.grizzly.asyncqueue.PushBackHandler}, which will be notified
     *        if message was accepted by transport write queue or refused
     * @deprecated push back logic is deprecated
     */
     <M> void write(M message,
            CompletionHandler<WriteResult<M, L>> completionHandler,
            org.glassfish.grizzly.asyncqueue.PushBackHandler pushbackHandler);

    /**
     * Method writes the <tt>buffer</tt> to the specific address.
     *
     * @param dstAddress the destination address the <tt>buffer</tt> will be
     *        sent to
     * @param message the buffer, from which the data will be written
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when write will be completed
     */
     <M> void write(L dstAddress,
            M message,
            CompletionHandler<WriteResult<M, L>> completionHandler);

     /**
     * Method writes the <tt>buffer</tt> to the specific address.
     *
     * @param dstAddress the destination address the <tt>buffer</tt> will be
     *        sent to
     * @param message the buffer, from which the data will be written
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when write will be completed
     * @param pushbackHandler {@link org.glassfish.grizzly.asyncqueue.PushBackHandler}, which will be notified
     *        if message was accepted by transport write queue or refused
     * @deprecated push back logic is deprecated
     */
     <M> void write(L dstAddress,
            M message,
            CompletionHandler<WriteResult<M, L>> completionHandler,
            org.glassfish.grizzly.asyncqueue.PushBackHandler pushbackHandler);
}
