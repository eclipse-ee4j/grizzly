/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.asyncqueue;

/**
 * Context being passed when {@link org.glassfish.grizzly.Writer} refuses to accept passed {@link WritableMessage} due
 * to I/O or memory limitations. User may perform one of the actions proposed by the context: 1) {@link #cancel()} to
 * cancel message writing 2) {@link #retryWhenPossible()} to ask Grizzly to write the message once it's possible 3)
 * {@link #retryNow()} to ask Grizzly to try to write message again (not suggested)
 *
 * @since 2.2
 *
 * @deprecated push back logic is deprecated.
 *
 * @author Alexey Stashok
 */
@Deprecated
public abstract class PushBackContext {
    protected final AsyncWriteQueueRecord queueRecord;

    public PushBackContext(final AsyncWriteQueueRecord queueRecord) {
        this.queueRecord = queueRecord;
    }

    /**
     * The {@link PushBackHandler} passed along with one of the {@link org.glassfish.grizzly.Writer}'s write(...) method
     * call.
     *
     * @return {@link PushBackHandler} passed along with write(...) call.
     */
    public PushBackHandler getPushBackHandler() {
        return queueRecord.getPushBackHandler();
    }

    /**
     * Returns the message size.
     *
     * @return the message size.
     */
    public final long size() {
        return queueRecord.remaining();
    }

    /**
     * Instructs Grizzly to send this message once some resources get released.
     */
    public abstract void retryWhenPossible();

    /**
     * Instructs Grizzly to try to resend the message right now.
     */
    public abstract void retryNow();

    /**
     * Instructs Grizzly to cancel this message write and release message associated resources.
     */
    public abstract void cancel();

}
