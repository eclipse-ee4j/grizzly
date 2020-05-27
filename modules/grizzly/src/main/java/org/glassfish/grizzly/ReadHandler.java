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

package org.glassfish.grizzly;

/**
 * <p>
 * This class represents a call-back mechanism that will notify implementations as more input data becomes available to
 * read without blocking.
 * </p>
 *
 * @since 2.2
 */
public interface ReadHandler {

    /**
     * <p>
     * Invoked when data is available to be read without blocking. Data must be consumed by the handler implementation
     * before re-registering.
     * </p>
     *
     * @throws Exception {@link Exception} might be thrown by the custom handler code. This exception will be delegated for
     * processing to {@link #onError(java.lang.Throwable)}.
     */
    void onDataAvailable() throws Exception;

    /**
     * <p>
     * Invoked when an error occurs processing the request asynchronously.
     * </p>
     * 
     * @param t the error
     */
    void onError(final Throwable t);

    /**
     * <p>
     * Invoked when all data for the current request has been read.
     * </p>
     *
     * @throws Exception {@link Exception} might be thrown by the custom handler code. This exception will be delegated for
     * processing to {@link #onError(java.lang.Throwable)}.
     */
    void onAllDataRead() throws Exception;

}
