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

import org.glassfish.grizzly.asyncqueue.MessageCloner;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.utils.Futures;

/**
 * Abstract class, which provides transitive dependencies for overloaded {@link Writer} methods.
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("deprecation")
public abstract class AbstractWriter<L> implements Writer<L> {

    /**
     * {@inheritDoc}
     */
    @Override
    public final GrizzlyFuture<WriteResult<WritableMessage, L>> write(final Connection<L> connection, final WritableMessage message) {
        return write(connection, null, message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void write(final Connection<L> connection, final WritableMessage message,
            final CompletionHandler<WriteResult<WritableMessage, L>> completionHandler) {
        write(connection, null, message, completionHandler, (MessageCloner) null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final GrizzlyFuture<WriteResult<WritableMessage, L>> write(final Connection<L> connection, final L dstAddress, final WritableMessage message) {
        final FutureImpl<WriteResult<WritableMessage, L>> future = Futures.createSafeFuture();

        write(connection, dstAddress, message, Futures.toCompletionHandler(future), (MessageCloner) null);

        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final void write(final Connection<L> connection, final L dstAddress, final WritableMessage message,
            final CompletionHandler<WriteResult<WritableMessage, L>> completionHandler) {
        write(connection, dstAddress, message, completionHandler, (MessageCloner) null);
    }

}
