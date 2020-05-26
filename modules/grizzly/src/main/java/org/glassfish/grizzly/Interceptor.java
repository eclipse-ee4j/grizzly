/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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
 * Handler, which is used to finer control {@link Readable}.
 *
 * @author Alexey Stashok
 */
public interface Interceptor<R> {
    int DEFAULT = 0;
    int COMPLETED = 1;
    int INCOMPLETED = 2;
    int RESET = 4;

    /**
     * Callback method is called by {@link Readable}, so it is possible to customize reading process. Mostly
     * {@link Interceptor} is used to control asynchronous reads.
     *
     * @param event type of intercepted event.
     * @param context read operation context.
     * @param result last read operation result.
     *
     * @return the implementation specific code to instruct {@link Readable} how it should continue reading operation.
     */
    int intercept(int event, Object context, R result);
}
