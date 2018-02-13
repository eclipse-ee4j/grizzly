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

import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

/**
 * Abstract class, which provides transitive dependencies for overloaded
 * {@link Reader} methods.
 * 
 * @author Alexey Stashok
 */
public abstract class AbstractReader<L> implements Reader<L> {
    /**
     * {@inheritDoc}
     */
    @Override
    public final GrizzlyFuture<ReadResult<Buffer, L>> read(
            final Connection<L> connection) {
        return read(connection, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final GrizzlyFuture<ReadResult<Buffer, L>> read(
            final Connection<L> connection,
            final Buffer buffer) {
        final FutureImpl<ReadResult<Buffer, L>> future =
                Futures.createSafeFuture();
        
        read(connection, buffer, Futures.toCompletionHandler(future), null);
        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void read(final Connection<L> connection, final Buffer buffer,
            final CompletionHandler<ReadResult<Buffer, L>> completionHandler) {
        read(connection, buffer, completionHandler, null);
    }
}
