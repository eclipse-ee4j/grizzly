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
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOEventLifeCycleListener;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.SelectorRunner;

/**
 * {@link org.glassfish.grizzly.IOStrategy}, which executes {@link org.glassfish.grizzly.Processor}s in a current
 * threads, and resumes selector thread logic in separate thread.
 *
 * @author Alexey Stashok
 */
public final class LeaderFollowerNIOStrategy extends AbstractIOStrategy {

    private static final LeaderFollowerNIOStrategy INSTANCE = new LeaderFollowerNIOStrategy();

    private static final Logger logger = Grizzly.logger(LeaderFollowerNIOStrategy.class);

    // ------------------------------------------------------------ Constructors

    private LeaderFollowerNIOStrategy() {
    }

    // ---------------------------------------------------------- Public Methods

    public static LeaderFollowerNIOStrategy getInstance() {

        return INSTANCE;

    }

    // ------------------------------------------------- Methods from IOStrategy

    @Override
    public boolean executeIoEvent(final Connection connection, final IOEvent ioEvent, final boolean isIoEventEnabled) throws IOException {

        final NIOConnection nioConnection = (NIOConnection) connection;
        IOEventLifeCycleListener listener = null;
        if (isReadWrite(ioEvent)) {
            if (isIoEventEnabled) {
                connection.disableIOEvent(ioEvent);
            }

            listener = ENABLE_INTEREST_LIFECYCLE_LISTENER;
        }

        final Executor threadPool = getThreadPoolFor(connection, ioEvent);
        if (threadPool != null) {
            final SelectorRunner runner = nioConnection.getSelectorRunner();
            runner.postpone();
            threadPool.execute(runner);
            fireIOEvent(connection, ioEvent, listener, logger);

            return false;
        } else {
            fireIOEvent(connection, ioEvent, listener, logger);
            return true;
        }
    }

}
