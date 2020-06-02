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

package org.glassfish.grizzly.impl;

import java.util.concurrent.Future;

import org.glassfish.grizzly.GrizzlyFuture;

/**
 * {@link Future} interface, which has full control over the state.
 *
 * @see Future
 *
 * @author Alexey Stashok
 */
public interface FutureImpl<R> extends GrizzlyFuture<R> {
    /**
     * Get current result value without any blocking.
     *
     * @return current result value without any blocking.
     */
    R getResult();

    /**
     * Set the result value and notify about operation completion.
     *
     * @param result the result value
     */
    void result(R result);

    /**
     * Notify about the failure, occurred during asynchronous operation execution.
     *
     * @param failure
     */
    void failure(Throwable failure);
}
