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

/**
 * <p>
 * This interface defines methods to allow an {@link java.io.OutputStream} or {@link java.io.Writer} to allow the
 * developer to check with the runtime whether or not it's possible to write a certain amount of data, or if it's not
 * possible, to be notified when it is.
 * </p>
 *
 * @since 2.0
 */
public interface OutputSink {

    /**
     * Instructs the <code>OutputSink</code> to invoke the provided {@link WriteHandler} when it is possible to write more
     * bytes (or characters).
     *
     * Note that once the {@link WriteHandler} has been notified, it will not be considered for notification again at a
     * later point in time.
     *
     * @param handler the {@link WriteHandler} that should be notified when it's possible to write more data.
     *
     * @throws IllegalStateException if this method is invoked and a handler from a previous invocation is still present
     * (due to not having yet been notified).
     *
     * @since 2.3
     */
    void notifyCanWrite(final WriteHandler handler);

    /**
     * Instructs the <code>OutputSink</code> to invoke the provided {@link WriteHandler} when it is possible to write
     * <code>length</code> bytes (or characters).
     *
     * Note that once the {@link WriteHandler} has been notified, it will not be considered for notification again at a
     * later point in time.
     *
     * @param handler the {@link WriteHandler} that should be notified when it's possible to write <code>length</code>
     * bytes.
     * @param length the number of bytes or characters that require writing.
     *
     * @throws IllegalStateException if this method is invoked and a handler from a previous invocation is still present
     * (due to not having yet been notified).
     *
     * @deprecated the <code>length</code> parameter will be ignored. Pls. use
     * {@link #notifyCanWrite(org.glassfish.grizzly.WriteHandler)}.
     */
    @Deprecated
    void notifyCanWrite(final WriteHandler handler, final int length);

    /**
     * @return <code>true</code> if a write to this <code>OutputSink</code> will succeed, otherwise returns
     * <code>false</code>.
     *
     * @since 2.3
     */
    boolean canWrite();

    /**
     * @param length specifies the number of bytes (or characters) that require writing
     *
     * @return <code>true</code> if a write to this <code>OutputSink</code> will succeed, otherwise returns
     * <code>false</code>.
     *
     * @deprecated the <code>length</code> parameter will be ignored. Pls. use {@link #canWrite()}.
     */
    @Deprecated
    boolean canWrite(final int length);

}
