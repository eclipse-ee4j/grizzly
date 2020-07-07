/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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
 * Grizzly {@link Future} implementation. Users can register additional {@link CompletionHandler}s using
 * {@link #addCompletionHandler(org.glassfish.grizzly.CompletionHandler)} to be notified once the asynchronous
 * computation, represented by this <tt>Future</tt>, is complete.
 *
 * A <tt>GrizzlyFuture</tt> instance can be recycled and reused.
 *
 * @param <R> the result type
 *
 * @author Alexey Stashok
 */
public interface GrizzlyFuture<R> extends Future<R>, Cacheable {
    /**
     * Adds a {@link CompletionHandler}, which will be notified once the asynchronous computation, represented by this
     * <tt>Future</tt>, is complete.
     *
     * @param completionHandler {@link CompletionHandler}
     * @since 2.3.4
     */
    void addCompletionHandler(CompletionHandler<R> completionHandler);

    /**
     * Mark <tt>GrizzlyFuture</tt> as recyclable, so once result will come - <tt>GrizzlyFuture</tt> object will be recycled
     * and returned to a thread local object pool. You can consider to use this method, if you're not interested in using
     * this <tt>GrizzlyFuture</tt> object.
     *
     * @param recycleResult if <tt>true</tt> - the <tt>GrizzlyFuture</tt> result, if it support recyclable mechanism, will
     * be also recycled together with this <tt>GrizzlyFuture</tt> object.
     *
     * @deprecated
     */
    @Deprecated
    void markForRecycle(boolean recycleResult);

    /**
     * Recycle <tt>GrizzlyFuture</tt> now. This method could be used, if you're not interested in using this
     * <tt>GrizzlyFuture</tt> object, and you're sure this object is not used by any other application part.
     *
     * @param recycleResult if <tt>true</tt> - the <tt>GrizzlyFuture</tt> result, if it support recyclable mechanism, will
     * be also recycled together with this <tt>GrizzlyFuture</tt> object.
     */
    void recycle(boolean recycleResult);
}
