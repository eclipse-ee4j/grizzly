/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server;

import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.CompletionHandler;

/**
 * Interface represents a context of the suspended {@link Response}.
 * 
 * @author Alexey Stashok
 */
public interface SuspendContext {

    /**
     * Get the suspended {@link Response} {@link CompletionHandler}.
     *
     * @return the suspended {@link Response} {@link CompletionHandler}.
     */
    CompletionHandler<Response> getCompletionHandler();

    /**
     * Get the suspended {@link Response} {@link TimeoutHandler}.
     *
     * @return the suspended {@link Response} {@link TimeoutHandler}.
     */
    TimeoutHandler getTimeoutHandler();

    /**
     * Get the suspended {@link Response} timeout. If returned value less or equal
     * to zero - timeout is not set.
     *
     * @return the suspended {@link Response} timeout. If returned value less or equal
     * to zero - timeout is not set.
     */
    long getTimeout(TimeUnit timeunit);

    /**
     * Set the suspended {@link Response} timeout. If timeout value less or equal
     * to zero - suspended {@link Response} won't be never timed out.
     *
     * @param timeout the suspended {@link Response} timeout.
     * @param timeunit timeout units.
     */
    void setTimeout(long timeout, TimeUnit timeunit);

    /**
     * Returns <tt>true</tt>, if the {@link Response} is suspended, or
     * <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt>, if the {@link Response} is suspended, or
     * <tt>false</tt> otherwise.
     */
    boolean isSuspended();
}
