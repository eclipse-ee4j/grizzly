/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.strategies.WorkerThreadPoolConfigProducer;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * <tt>strategy</tt> is responsible for making decision how
 * {@link Runnable} task will be run: in current thread, worker thread.
 *
 * <tt>strategy</tt> can make any other processing decisions.
 * 
 * @author Alexey Stashok
 */
public interface IOStrategy extends WorkerThreadPoolConfigProducer {

    /**
     * The {@link org.glassfish.grizzly.nio.SelectorRunner} will invoke this
     * method to allow the strategy implementation to decide how the
     * {@link IOEvent} will be handled.
     *
     * @param connection the {@link Connection} upon which the provided
     *  {@link IOEvent} occurred.
     * @param ioEvent the {@link IOEvent} that triggered execution of this
     *  <code>strategy</code>
     *
     * @return <tt>true</tt>, if this thread should keep processing IOEvents on
     * the current and other Connections, or <tt>false</tt> if this thread
     * should hand-off the farther IOEvent processing on any Connections,
     * which means IOStrategy is becoming responsible for continuing IOEvent
     * processing (possibly starting new thread, which will handle IOEvents).
     *
     * @throws IOException if an error occurs processing the {@link IOEvent}.
     */
    boolean executeIoEvent(Connection connection, IOEvent ioEvent)
    throws IOException;

    /**
     * The {@link org.glassfish.grizzly.nio.SelectorRunner} will invoke this
     * method to allow the strategy implementation to decide how the
     * {@link IOEvent} will be handled.
     *
     * @param connection the {@link Connection} upon which the provided
     *  {@link IOEvent} occurred.
     * @param ioEvent the {@link IOEvent} that triggered execution of this
     *  <code>strategy</code>
     * @param isIoEventEnabled <tt>true</tt> if IOEvent is still enabled on the
     *  {@link Connection}, or <tt>false</tt> if IOEvent was preliminary disabled
     *  or IOEvent is being simulated.
     *
     * @return <tt>true</tt>, if this thread should keep processing IOEvents on
     * the current and other Connections, or <tt>false</tt> if this thread
     * should hand-off the farther IOEvent processing on any Connections,
     * which means IOStrategy is becoming responsible for continuing IOEvent
     * processing (possibly starting new thread, which will handle IOEvents).
     *
     * @throws IOException if an error occurs processing the {@link IOEvent}.
     */
    boolean executeIoEvent(Connection connection, IOEvent ioEvent,
            boolean isIoEventEnabled) throws IOException;
    
    /**
     * Returns an {@link Executor} to be used to run given <tt>ioEvent</tt>
     * processing for the given <tt>connection</tt>. A <tt>null</tt> value will
     * be returned if the <tt>ioEvent</tt> should be executed in the kernel thread.
     * 
     * @param connection
     * @param ioEvent
     * @return an {@link Executor} to be used to run given <tt>ioEvent</tt>
     * processing for the given <tt>connection</tt>
     */
    Executor getThreadPoolFor(Connection connection, IOEvent ioEvent);
}
