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

package org.glassfish.grizzly.strategies;

import java.io.IOException;
import java.util.concurrent.Executor;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOEventLifeCycleListener;
import org.glassfish.grizzly.Processor;
import java.util.logging.Logger;

/**
 * {@link org.glassfish.grizzly.IOStrategy}, which executes {@link Processor}s in worker thread.
 *
 * @author Alexey Stashok
 */
public final class WorkerThreadIOStrategy extends AbstractIOStrategy {

    private static final WorkerThreadIOStrategy INSTANCE = new WorkerThreadIOStrategy();

    private static final Logger logger = Grizzly.logger(WorkerThreadIOStrategy.class);


    // ------------------------------------------------------------ Constructors


    private WorkerThreadIOStrategy() { }


    // ---------------------------------------------------------- Public Methods


    public static WorkerThreadIOStrategy getInstance() {
        return INSTANCE;
    }


    // ------------------------------------------------- Methods from IOStrategy


    @Override
    public boolean executeIoEvent(final Connection connection,
            final IOEvent ioEvent, final boolean isIoEventEnabled)
            throws IOException {

        final boolean isReadOrWriteEvent = isReadWrite(ioEvent);

        final IOEventLifeCycleListener listener;
        if (isReadOrWriteEvent) {
            if (isIoEventEnabled) {
                connection.disableIOEvent(ioEvent);
            }
            
            listener = ENABLE_INTEREST_LIFECYCLE_LISTENER;
        } else {
            listener = null;
        }

        final Executor threadPool = getThreadPoolFor(connection, ioEvent);
        if (threadPool != null) {
            threadPool.execute(
                    new WorkerThreadRunnable(connection, ioEvent, listener));
        } else {
            run0(connection, ioEvent, listener);
        }

        return true;
    }


    // --------------------------------------------------------- Private Methods


    private static void run0(final Connection connection,
                             final IOEvent ioEvent,
                             final IOEventLifeCycleListener lifeCycleListener) {

        fireIOEvent(connection, ioEvent, lifeCycleListener, logger);

    }
    
    private static final class WorkerThreadRunnable implements Runnable {
        final Connection connection;
        final IOEvent ioEvent;
        final IOEventLifeCycleListener lifeCycleListener;
        
        private WorkerThreadRunnable(final Connection connection,
                final IOEvent ioEvent,
                final IOEventLifeCycleListener lifeCycleListener) {
            this.connection = connection;
            this.ioEvent = ioEvent;
            this.lifeCycleListener = lifeCycleListener;
            
        }

        @Override
        public void run() {
            run0(connection, ioEvent, lifeCycleListener);
        }        
    }

}
