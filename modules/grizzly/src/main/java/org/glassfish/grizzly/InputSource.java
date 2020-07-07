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

import java.io.InputStream;
import java.io.Reader;

/**
 * <p>
 * This interface defines methods to allow an {@link InputStream} or {@link Reader} to notify the developer
 * <em>when</em> and <em>how much</em> data is ready to be read without blocking.
 * </p>
 *
 * @since 2.0
 */
public interface InputSource {

    /**
     * <p>
     * Notify the specified {@link ReadHandler} when any number of bytes or characters can be read without blocking.
     * </p>
     *
     * <p>
     * Invoking this method is equivalent to calling: notifyAvailable(handler, 1).
     * </p>
     *
     * @param handler the {@link ReadHandler} to notify.
     *
     * @throws IllegalArgumentException if <code>handler</code> is <code>null</code>.
     * @throws IllegalStateException if an attempt is made to register a handler before an existing registered handler has
     * been invoked or if all request data has already been read.
     *
     * @see ReadHandler#onDataAvailable()
     * @see ReadHandler#onAllDataRead()
     */
    void notifyAvailable(final ReadHandler handler);

    /**
     * <p>
     * Notify the specified {@link ReadHandler} when the number of bytes that can be read without blocking is greater or
     * equal to the specified <code>size</code>.
     * </p>
     *
     * @param handler the {@link ReadHandler} to notify.
     * @param size the least number of bytes that must be available before the {@link ReadHandler} is invoked.
     *
     * @throws IllegalArgumentException if <code>handler</code> is <code>null</code>, or if <code>size</code> is less or
     * equal to zero.
     * @throws IllegalStateException if an attempt is made to register a handler before an existing registered handler has
     * been invoked or if all request data has already been read.
     *
     * @see ReadHandler#onDataAvailable()
     * @see ReadHandler#onAllDataRead()
     */
    void notifyAvailable(final ReadHandler handler, final int size);

    /**
     * @return <code>true</code> when all data for this particular request has been read, otherwise returns
     * <code>false</code>.
     */
    boolean isFinished();

    /**
     * @return the number of bytes (or characters) that may be obtained without blocking. Note when dealing with characters,
     * this method may return an estimate on the number of characters available.
     */
    int readyData();

    /**
     * @return <code>true</code> if data can be obtained without blocking, otherwise returns <code>false</code>.
     */
    boolean isReady();

}
