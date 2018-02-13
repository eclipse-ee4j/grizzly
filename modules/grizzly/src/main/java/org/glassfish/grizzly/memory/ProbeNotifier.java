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

package org.glassfish.grizzly.memory;

import org.glassfish.grizzly.monitoring.DefaultMonitoringConfig;

/**
 * Utility class, which has notification methods for different
 * {@link MemoryProbe} events.
 *
 * @author Alexey Stashok
 */
final class ProbeNotifier {

    /**
     * Notify registered {@link MemoryProbe}s about the "allocated" event.
     *
     * @param size buffer size
     */
    static void notifyBufferAllocated(
            final DefaultMonitoringConfig<MemoryProbe> config,
            final int size) {

        final MemoryProbe[] probes = config.getProbesUnsafe();
        if (probes != null) {
            for (MemoryProbe probe : probes) {
                probe.onBufferAllocateEvent(size);
            }
        }
    }

    /**
     * Notify registered {@link MemoryProbe}s about the "allocated from pool" event.
     *
     * @param size buffer size
     */
    static void notifyBufferAllocatedFromPool(
            final DefaultMonitoringConfig<MemoryProbe> config,
            final int size) {

        final MemoryProbe[] probes = config.getProbesUnsafe();
        if (probes != null) {
            for (MemoryProbe probe : probes) {
                probe.onBufferAllocateFromPoolEvent(size);
            }
        }
    }

    /**
     * Notify registered {@link MemoryProbe}s about the "release to pool" event.
     *
     * @param size buffer size
     */
    static void notifyBufferReleasedToPool(
            final DefaultMonitoringConfig<MemoryProbe> config,
            final int size) {

        final MemoryProbe[] probes = config.getProbesUnsafe();
        if (probes != null) {
            for (MemoryProbe probe : probes) {
                probe.onBufferReleaseToPoolEvent(size);
            }
        }
    }

}
