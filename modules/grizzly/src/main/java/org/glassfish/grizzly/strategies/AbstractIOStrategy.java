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

package org.glassfish.grizzly.strategies;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.*;
import org.glassfish.grizzly.asyncqueue.AsyncQueue;
import org.glassfish.grizzly.localization.LogMessages;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 *
 * @author oleksiys
 */
public abstract class AbstractIOStrategy implements IOStrategy {

    private final static EnumSet<IOEvent> READ_WRITE_EVENT_SET =
            EnumSet.of(IOEvent.READ, IOEvent.WRITE);

    private final static EnumSet<IOEvent> WORKER_THREAD_EVENT_SET =
            EnumSet.of(IOEvent.READ, IOEvent.CLOSED);
    
    protected final static IOEventLifeCycleListener ENABLE_INTEREST_LIFECYCLE_LISTENER =
            new EnableInterestLifeCycleListener();


    // ----------------------------- Methods from WorkerThreadPoolConfigProducer


    @Override
    public ThreadPoolConfig createDefaultWorkerPoolConfig(final Transport transport) {

        final ThreadPoolConfig config = ThreadPoolConfig.defaultConfig().copy();
        final int coresCount = Runtime.getRuntime().availableProcessors();
        config.setPoolName("Grizzly-worker");
        config.setCorePoolSize(coresCount * 2);
        config.setMaxPoolSize(coresCount * 2);
        config.setMemoryManager(transport.getMemoryManager());
        return config;

    }

    // ------------------------------------------------------- Public Methods

    @Override
    public final boolean executeIoEvent(final Connection connection,
            final IOEvent ioEvent) throws IOException {
        return executeIoEvent(connection, ioEvent, true);
    }

    @Override
    public Executor getThreadPoolFor(final Connection connection,
            final IOEvent ioEvent) {
        return WORKER_THREAD_EVENT_SET.contains(ioEvent) ?
                connection.getTransport().getWorkerThreadPool() :
                null;
    }

    // ------------------------------------------------------- Protected Methods


    protected static boolean isReadWrite(final IOEvent ioEvent) {
        return READ_WRITE_EVENT_SET.contains(ioEvent);
    }

    protected static void fireIOEvent(final Connection connection,
                                      final IOEvent ioEvent,
                                      final IOEventLifeCycleListener listener,
                                      final Logger logger) {
        try {
            connection.getTransport().fireIOEvent(ioEvent, connection, listener);
        } catch (Exception e) {
            logger.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_IOSTRATEGY_UNCAUGHT_EXCEPTION(), e);
            connection.closeSilently();
        }

    }


    // ---------------------------------------------------------- Nested Classes


    private final static class EnableInterestLifeCycleListener
            extends IOEventLifeCycleListener.Adapter {
        
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
