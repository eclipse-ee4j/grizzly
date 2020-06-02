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

/**
 * Interface, which will be used by Grizzly to notify about asynchronous I/O operations status updates.
 *
 * @param <E> the type of the result
 *
 * @author Alexey Stashok
 */
public interface CompletionHandler<E> {
    /**
     * The operation was cancelled.
     */
    void cancelled();

    /**
     * The operation was failed.
     * 
     * @param throwable error, which occurred during operation execution
     */
    void failed(Throwable throwable);

    /**
     * The operation was completed.
     * 
     * @param result the operation result
     *
     * Please note, for performance reasons the result object might be recycled after returning from the completed method.
     * So it's not guaranteed that using of the result object is safe outside this method's scope.
     */
    void completed(E result);

    /**
     * The callback method may be called, when there is some progress in operation execution, but it is still not completed
     * 
     * @param result the current result
     *
     * Please note, for performance reasons the result object might be recycled after returning from the updated method. So
     * it's not guaranteed that using of the result object is safe outside this method's scope.
     */
    void updated(E result);
}
