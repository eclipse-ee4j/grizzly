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

package org.glassfish.grizzly.strategies;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Context;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOEventLifeCycleListener;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.asyncqueue.AsyncQueue;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * {@link org.glassfish.grizzly.IOStrategy}, which executes {@link org.glassfish.grizzly.Processor}s in a current
 * thread.
 *
 * @author Alexey Stashok
 */
public final class SameThreadIOStrategy extends AbstractIOStrategy {

    private static final SameThreadIOStrategy INSTANCE = new SameThreadIOStrategy();

    private static final Logger logger = Grizzly.logger(SameThreadIOStrategy.class);

    private static final InterestLifeCycleListenerWhenIoEnabled LIFECYCLE_LISTENER_WHEN_IO_ENABLED = new InterestLifeCycleListenerWhenIoEnabled();

    private static final InterestLifeCycleListenerWhenIoDisabled LIFECYCLE_LISTENER_WHEN_IO_DISABLED = new InterestLifeCycleListenerWhenIoDisabled();

    // ------------------------------------------------------------ Constructors

    private SameThreadIOStrategy() {
    }

    // ---------------------------------------------------------- Public Methods

    public static SameThreadIOStrategy getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------- Methods from IOStrategy

    @Override
    public boolean executeIoEvent(final Connection connection, final IOEvent ioEvent, final boolean isIoEventEnabled) throws IOException {

        IOEventLifeCycleListener listener = null;
        if (isReadWrite(ioEvent)) {
            listener = isIoEventEnabled ? LIFECYCLE_LISTENER_WHEN_IO_ENABLED : LIFECYCLE_LISTENER_WHEN_IO_DISABLED;
        }

        fireIOEvent(connection, ioEvent, listener, logger);

        return true;
    }

    @Override
    public Executor getThreadPoolFor(final Connection connection, final IOEvent ioEvent) {
        return null;
    }

    // ----------------------------------- Methods from WorkerThreadPoolConfigProducer

    @Override
    public ThreadPoolConfig createDefaultWorkerPoolConfig(final Transport transport) {
        return null;
    }

    // ---------------------------------------------------------- Nested Classes

    private static final class InterestLifeCycleListenerWhenIoEnabled extends IOEventLifeCycleListener.Adapter {

        @Override
        public void onReregister(final Context context) throws IOException {
            onComplete(context, null);
        }

        @Override
        public void onComplete(final Context context, final Object data) throws IOException {
            if (context.wasSuspended() || context.isManualIOEventControl()) {
                final IOEvent ioEvent = context.getIoEvent();
                final Connection connection = context.getConnection();

                if (AsyncQueue.EXPECTING_MORE_OPTION.equals(data)) {
                    connection.simulateIOEvent(ioEvent);
                } else {
                    connection.enableIOEvent(ioEvent);
                }
            }
        }

        @Override
        public void onContextSuspend(final Context context) throws IOException {
            // check manual io event control, to not disable ioevent twice
            if (!context.wasSuspended() && !context.isManualIOEventControl()) {
                disableIOEvent(context);
            }
        }

        @Override
        public void onContextManualIOEventControl(final Context context) throws IOException {
            // check suspended mode, to not disable ioevent twice
            if (!context.wasSuspended() && !context.isManualIOEventControl()) {
                disableIOEvent(context);
            }
        }

        private static void disableIOEvent(final Context context) throws IOException {
            final Connection connection = context.getConnection();
            final IOEvent ioEvent = context.getIoEvent();
            connection.disableIOEvent(ioEvent);
        }

    }

    private static final class InterestLifeCycleListenerWhenIoDisabled extends IOEventLifeCycleListener.Adapter {

        @Override
        public void onReregister(final Context context) throws IOException {
            onComplete(context, null);
        }

        @Override
        public void onComplete(final Context context, final Object data) throws IOException {

            final IOEvent ioEvent = context.getIoEvent();
            final Connection connection = context.getConnection();
            if (AsyncQueue.EXPECTING_MORE_OPTION.equals(data)) {
                connection.simulateIOEvent(ioEvent);
            } else {
                connection.enableIOEvent(ioEvent);
            }
        }
    }
}
