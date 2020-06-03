/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio.tmpselectors;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.localization.LogMessages;
import org.glassfish.grizzly.nio.Selectors;

/**
 *
 * @author oleksiys
 */
public class TemporarySelectorPool {
    private static final Logger LOGGER = Grizzly.logger(TemporarySelectorPool.class);

    public static final int DEFAULT_SELECTORS_COUNT = 32;

    private static final int MISS_THRESHOLD = 10000;

    /**
     * The max number of <code>Selector</code> to create.
     */
    private volatile int maxPoolSize;

    private final AtomicBoolean isClosed;

    /**
     * Cache of <code>Selector</code>
     */
    private final Queue<Selector> selectors;

    /**
     * The current number of Selectors in the pool.
     */
    private final AtomicInteger poolSize;

    /**
     * Number of times poll execution didn't find the available selector in the pool.
     */
    private final AtomicInteger missesCounter;

    private final SelectorProvider selectorProvider;

    public TemporarySelectorPool(final SelectorProvider selectorProvider) {
        this(selectorProvider, DEFAULT_SELECTORS_COUNT);
    }

    public TemporarySelectorPool(final SelectorProvider selectorProvider, final int selectorsCount) {
        this.selectorProvider = selectorProvider;
        this.maxPoolSize = selectorsCount;
        isClosed = new AtomicBoolean();
        selectors = new ConcurrentLinkedQueue<>();
        poolSize = new AtomicInteger();
        missesCounter = new AtomicInteger();
    }

    public synchronized int size() {
        return maxPoolSize;
    }

    public synchronized void setSize(int size) throws IOException {
        if (isClosed.get()) {
            return;
        }

        missesCounter.set(0);
        this.maxPoolSize = size;
    }

    public SelectorProvider getSelectorProvider() {
        return selectorProvider;
    }

    public Selector poll() throws IOException {
        Selector selector = selectors.poll();

        if (selector != null) {
            poolSize.decrementAndGet();
        } else {
            try {
                selector = Selectors.newSelector(selectorProvider);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_TEMPORARY_SELECTOR_POOL_CREATE_SELECTOR_EXCEPTION(), e);
            }

            final int missesCount = missesCounter.incrementAndGet();
            if (missesCount % MISS_THRESHOLD == 0) {
                LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_TEMPORARY_SELECTOR_POOL_MISSES_EXCEPTION(missesCount, maxPoolSize));
            }
        }

        return selector;
    }

    public void offer(Selector selector) {
        if (selector == null) {
            return;
        }

        final boolean wasReturned;

        if (poolSize.getAndIncrement() < maxPoolSize && (selector = checkSelector(selector)) != null) {

            selectors.offer(selector);
            wasReturned = true;
        } else {
            poolSize.decrementAndGet();

            if (selector == null) {
                return;
            }

            wasReturned = false;
        }

        if (isClosed.get()) {
            if (selectors.remove(selector)) {
                closeSelector(selector);
            }
        } else if (!wasReturned) {
            closeSelector(selector);
        }
    }

    public synchronized void close() {
        if (!isClosed.getAndSet(true)) {
            Selector selector;
            while ((selector = selectors.poll()) != null) {
                closeSelector(selector);
            }
        }
    }

    private void closeSelector(Selector selector) {
        try {
            selector.close();
        } catch (IOException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "TemporarySelectorFactory: error " + "occurred when trying to close the Selector", e);
            }
        }
    }

    private Selector checkSelector(final Selector selector) {
        try {
            selector.selectNow();
            return selector;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_TEMPORARY_SELECTOR_POOL_SELECTOR_FAILURE_EXCEPTION(), e);
            try {
                return Selectors.newSelector(selectorProvider);
            } catch (IOException ee) {
                LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_TEMPORARY_SELECTOR_POOL_CREATE_SELECTOR_EXCEPTION(), ee);
            }
        }

        return null;
    }
}
