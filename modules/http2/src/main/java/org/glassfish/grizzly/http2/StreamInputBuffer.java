/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import java.io.IOException;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.HttpContent;

/**
 *
 * @author oleksiys
 */
interface StreamInputBuffer {
    /**
     * The method will be invoked once upstream completes READ operation processing.
     * Here we have to simulate NIO OP_READ event re-registration.
     */
    void onReadEventComplete();

    /**
     * The method is called, when new input data arrives.
     */
    boolean offer(final Buffer data, final boolean isLast);

    /**
     * Retrieves available input buffer payload, waiting up to the
     * {@link Connection#getReadTimeout(java.util.concurrent.TimeUnit)}
     * wait time if necessary for payload to become available.
     * 
     * @throws IOException if an error occurs during the poll operation.
     */
    HttpContent poll() throws IOException;

    /**
     * Graceful input buffer close.
     * 
     * Marks the input buffer as closed by adding Termination input element to the input queue.
     */
    void close(final Termination termination);

    /**
     * Forcibly closes the input buffer.
     * 
     * All the buffered data will be discarded.
     */
    void terminate(final Termination termination);
    
    /**
     * Returns <tt>true</tt> if the <tt>InputBuffer</tt> has been closed.
     */
    boolean isClosed();
}
