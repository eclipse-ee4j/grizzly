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
 * Implementations of this interface are able to read data from
 * {@link Connection} to a {@link Buffer}.
 *
 * There are two basic Reader implementations in Grizzly:
 * {@link org.glassfish.grizzly.asyncqueue.AsyncQueueReader},
 * {@link org.glassfish.grizzly.nio.tmpselectors.TemporarySelectorReader}.
 *
 * @param <L> the reader address type
 * 
 * @author Alexey Stashok
 */
public interface Reader<L> {
    int READ_EVENT = 1;
    int COMPLETE_EVENT = 2;
    int INCOMPLETE_EVENT = 3;
    /**
     * Method reads data.
     *
     * @param connection the {@link Connection} to read from
     * @return {@link Future}, using which it's possible to check the result
     */
    GrizzlyFuture<ReadResult<Buffer, L>> read(Connection<L> connection);

    /**
     * Method reads data to the <tt>buffer</tt>.
     *
     * @param connection the {@link Connection} to read from
     * @param buffer the buffer, where data will be read
     * @return {@link Future}, using which it's possible to check the result
     */
    GrizzlyFuture<ReadResult<Buffer, L>> read(Connection<L> connection,
                                              Buffer buffer);

    /**
     * Method reads data to the <tt>buffer</tt>.
     *
     * @param connection the {@link Connection} to read from
     * @param buffer the buffer, where data will be read
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when read will be completed
     */
    void read(Connection<L> connection,
              Buffer buffer,
              CompletionHandler<ReadResult<Buffer, L>> completionHandler);


    /**
     * Method reads data to the <tt>buffer</tt>.
     *
     * @param connection the {@link Connection} to read from
     * @param buffer the {@link Buffer} to which data will be read
     * @param completionHandler {@link CompletionHandler},
     *        which will get notified, when read will be completed
     * @param interceptor {@link Interceptor}, which will be able to intercept
     *        control each time new portion of a data was read to a
     *        <tt>buffer</tt>.
     *        The <tt>interceptor</tt> can decide, whether asynchronous read is
     *        completed or not, or provide other processing instructions.
     */
    void read(Connection<L> connection,
              Buffer buffer,
              CompletionHandler<ReadResult<Buffer, L>> completionHandler,
              Interceptor<ReadResult> interceptor);
}
