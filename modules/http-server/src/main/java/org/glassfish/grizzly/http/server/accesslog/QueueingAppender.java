/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.accesslog;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;

/**
 * An {@link AccessLogAppender appender} enqueueing log entries into a
 * {@link LinkedBlockingQueue} and using a secondary, separate {@link Thread}
 * to forward them to a configured nested {@link AccessLogAppender appender}.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 * @author <a href="http://www.usrz.com/">USRZ.com</a>
 */
public class QueueingAppender implements AccessLogAppender {

    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /* Our queue */
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    /* Where to forward stuff to */
    private final AccessLogAppender appender;
    /* The thread doing the despooling */
    private final Thread thread;

    /**
     * Create a new {@link QueueingAppender} instance enqueueing log entries
     * into a {@link LinkedBlockingQueue} and dequeueing them using a secondary
     * separate {@link Thread}.
     */
    public QueueingAppender(AccessLogAppender appender) {
        if (appender == null) throw new NullPointerException("Null appender");
        this.appender = appender;

        thread = new Thread(new Dequeuer());
        thread.setName(toString());
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void append(String accessLogEntry)
    throws IOException {
        if (thread.isAlive()) try {
            queue.put(accessLogEntry);
        } catch (InterruptedException exception) {
            LOGGER.log(FINE, "Interrupted adding log entry to the queue", exception);
        }
    }

    @Override
    public void close() throws IOException {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException exception) {
            LOGGER.log(FINE, "Interrupted stopping de-queuer", exception);
        } finally {
            appender.close();
        }
    }

    /* ====================================================================== */
    /* OUR DE-QUEUER                                                          */
    /* ====================================================================== */

    private final class Dequeuer implements Runnable {
        @Override
        public void run() {
            while (true) try {
                final String accessLogEntry = queue.take();
                if (accessLogEntry != null) appender.append(accessLogEntry);
            } catch (InterruptedException exception) {
                LOGGER.log(FINE, "Interrupted waiting for log entry to be queued, exiting!", exception);
                return;
            } catch (Throwable throwable) {
                LOGGER.log(WARNING, "Exception caught appending ququed log entry", throwable);
            }
        }
    }

}
