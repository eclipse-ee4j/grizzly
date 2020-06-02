/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.filterchain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * An event that {@link Filter} implementations may listen for if special processing is required during a graceful
 * shutdown.
 *
 * @since 2.4.0
 */
public class ShutdownEvent implements FilterChainEvent {

    public static final Object TYPE = ShutdownEvent.class.getName();
    private Set<Callable<Filter>> shutdownFutures;
    private long gracePeriod;
    private TimeUnit timeUnit;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a new {@link ShutdownEvent} with the grace period for the shutdown.
     */
    public ShutdownEvent(final long gracePeriod, final TimeUnit timeUnit) {
        this.gracePeriod = gracePeriod;
        this.timeUnit = timeUnit;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Object type() {
        return TYPE;
    }

    /**
     * Adds a task to this event. Tasks should be called on separate threads after all {@link Filter}s in the chain have
     * been notified of the impending shutdown.
     */
    public void addShutdownTask(final Callable<Filter> future) {
        if (future == null) {
            return;
        }
        if (shutdownFutures == null) {
            shutdownFutures = new LinkedHashSet<>(4);
        }
        shutdownFutures.add(future);
    }

    /**
     * @return a {@link Set} of {@link Callable<Filter>} instances that need to be checked in order to proceed with
     * terminating processing.
     */
    public Set<Callable<Filter>> getShutdownTasks() {
        return shutdownFutures != null ? shutdownFutures : Collections.emptySet();
    }

    /**
     * @return the shutdown grace period.
     */
    public long getGracePeriod() {
        return gracePeriod;
    }

    /**
     * @return the {@link TimeUnit} of the grace period.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
