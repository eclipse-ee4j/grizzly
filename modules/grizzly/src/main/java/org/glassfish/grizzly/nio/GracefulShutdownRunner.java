/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.GracefulShutdownListener;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ShutdownContext;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.localization.LogMessages;

class GracefulShutdownRunner implements Runnable {
    private static final Logger LOGGER = Grizzly.logger(GracefulShutdownRunner.class);

    private final NIOTransport transport;
    private final Set<GracefulShutdownListener> shutdownListeners;
    private final ExecutorService shutdownService;
    private final long gracePeriod;
    private final TimeUnit timeUnit;

    // -------------------------------------------------------- Constructors
    GracefulShutdownRunner(final NIOTransport transport,
            final Set<GracefulShutdownListener> shutdownListeners,
            final ExecutorService shutdownService,
            final long gracePeriod, final TimeUnit timeUnit) {
        this.transport = transport;
        this.shutdownListeners = shutdownListeners;
        this.shutdownService = shutdownService;
        this.gracePeriod = gracePeriod;
        this.timeUnit = timeUnit;
    }

    // ----------------------------------------------- Methods from Runnable
    @Override
    public void run() {
        final int listenerCount = shutdownListeners.size();
        final CountDownLatch shutdownLatch = new CountDownLatch(listenerCount);

        // If there there is no timeout, invoke the listeners in the
        // same thread otherwise use one additional thread to invoke them.
        final Map<ShutdownContext,GracefulShutdownListener> contexts =
                new HashMap<ShutdownContext,GracefulShutdownListener>(listenerCount);
        if (gracePeriod <= 0) {
            for (final GracefulShutdownListener l : shutdownListeners) {
                final ShutdownContext ctx = createContext(contexts, l, shutdownLatch);
                l.shutdownRequested(ctx);
            }
        } else {
            shutdownService.execute(new Runnable() {
                @Override
                public void run() {
                    for (final GracefulShutdownListener l : shutdownListeners) {
                        final ShutdownContext ctx = createContext(contexts, l, shutdownLatch);
                        l.shutdownRequested(ctx);
                    }
                }
            });
        }
        try {
            if (gracePeriod <= 0) {
                shutdownLatch.await();
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING,
                            LogMessages.WARNING_GRIZZLY_GRACEFULSHUTDOWN_MSG(
                                    transport.getName() + '[' + Integer.toHexString(hashCode()) + ']',
                                    gracePeriod, timeUnit));
                }
                final boolean result = shutdownLatch.await(gracePeriod, timeUnit);
                if (!result) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING,
                                LogMessages.WARNING_GRIZZLY_GRACEFULSHUTDOWN_EXCEEDED(
                                        transport.getName() + '[' + Integer.toHexString(hashCode()) + ']'));
                    }
                    if (!contexts.isEmpty()) {
                        for (GracefulShutdownListener l : contexts.values()) {
                            l.shutdownForced();
                        }
                    }
                }
            }
        } catch (InterruptedException ie) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(LogMessages.WARNING_GRIZZLY_GRACEFULSHUTDOWN_INTERRUPTED());
            }
            if (!contexts.isEmpty()) {
                for (GracefulShutdownListener l : contexts.values()) {
                    l.shutdownForced();
                }
            }
        } finally {
            final Lock lock = transport.getState().getStateLocker().writeLock();
            lock.lock();
            
            try {
                // Make sure the transport is still expecting to be shutdown
                if (transport.shutdownService == this.shutdownService) {
                    transport.finalizeShutdown();
                }
            } finally {
                lock.unlock();
            }
        }
    }

    private ShutdownContext createContext(final Map<ShutdownContext,GracefulShutdownListener> contexts,
                                          final GracefulShutdownListener listener,
                                          final CountDownLatch shutdownLatch) {
        final ShutdownContext ctx = new ShutdownContext() {
            boolean isNotified;

            @Override
            public Transport getTransport() {
                return transport;
            }

            @Override
            public synchronized void ready() {
                if (!isNotified) {
                    isNotified = true;
                    contexts.remove(this);
                    shutdownLatch.countDown();
                }
            }
        };
        contexts.put(ctx, listener);
        return ctx;
    }

} // END GracefulShutdownRunner
