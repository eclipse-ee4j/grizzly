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

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOStrategy;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.NIOTransport;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;

/**
 * Simple dynamic strategy, which switches I/O processing strategies, basing on statistics. This implementation takes in
 * consideration number of {@link java.nio.channels.SelectionKey}s, which were selected last time by
 * {@link java.nio.channels.Selector}.
 *
 * <tt>SimpleDynamicIOStrategy</tt> is able to use 2 strategies underneath: {@link SameThreadIOStrategy},
 * {@link WorkerThreadIOStrategy}. And is able to switch between them basing on corresponding threshold (threshold
 * represents the number of selected {@link java.nio.channels.SelectionKey}s).
 *
 * So the strategy is getting applied following way:
 *
 * {@link SameThreadIOStrategy} --(worker-thread threshold)--> {@link WorkerThreadIOStrategy}.
 *
 * @author Alexey Stashok
 */
public final class SimpleDynamicNIOStrategy implements IOStrategy {

    private static final SimpleDynamicNIOStrategy INSTANCE = new SimpleDynamicNIOStrategy();

    private final SameThreadIOStrategy sameThreadStrategy;
    private final WorkerThreadIOStrategy workerThreadStrategy;

    private static final int WORKER_THREAD_THRESHOLD = 1;

    // ------------------------------------------------------------ Constructors

    private SimpleDynamicNIOStrategy() {
        sameThreadStrategy = SameThreadIOStrategy.getInstance();
        workerThreadStrategy = WorkerThreadIOStrategy.getInstance();
    }

    // ---------------------------------------------------------- Public Methods

    public static SimpleDynamicNIOStrategy getInstance() {

        return INSTANCE;

    }

    @Override
    public boolean executeIoEvent(Connection connection, IOEvent ioEvent) throws IOException {
        return executeIoEvent(connection, ioEvent, true);
    }

    // ------------------------------------------------- Methods from IOStrategy

    @Override
    public Executor getThreadPoolFor(final Connection connection, final IOEvent ioEvent) {
        final int lastSelectedKeysCount = getLastSelectedKeysCount(connection);

        return lastSelectedKeysCount <= WORKER_THREAD_THRESHOLD ? sameThreadStrategy.getThreadPoolFor(connection, ioEvent)
                : workerThreadStrategy.getThreadPoolFor(connection, ioEvent);
    }

    @Override
    public boolean executeIoEvent(final Connection connection, final IOEvent ioEvent, final boolean isIoEventEnabled) throws IOException {

        final int lastSelectedKeysCount = getLastSelectedKeysCount(connection);

        return lastSelectedKeysCount <= WORKER_THREAD_THRESHOLD ? sameThreadStrategy.executeIoEvent(connection, ioEvent, isIoEventEnabled)
                : workerThreadStrategy.executeIoEvent(connection, ioEvent, isIoEventEnabled);
    }

    @Override
    public ThreadPoolConfig createDefaultWorkerPoolConfig(final Transport transport) {

        final ThreadPoolConfig config = ThreadPoolConfig.defaultConfig().copy();
        final int selectorRunnerCount = ((NIOTransport) transport).getSelectorRunnersCount();
        config.setCorePoolSize(selectorRunnerCount * 2);
        config.setMaxPoolSize(selectorRunnerCount * 2);
        config.setMemoryManager(transport.getMemoryManager());
        return config;

    }

    // --------------------------------------------------------- Private Methods

    private static int getLastSelectedKeysCount(final Connection c) {
        return ((NIOConnection) c).getSelectorRunner().getLastSelectedKeysCount();
    }

}
