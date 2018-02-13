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

package org.glassfish.grizzly.monitoring;

import org.glassfish.grizzly.utils.ArraySet;

/**
 * Default monitoring configuration, with no JMX support.
 *
 * The {@link #createManagementObject()} method returns null, so if a child
 * class requires JMX support, it has to implement this method properly.
 * 
 * @author Alexey Stashok
 */
public class DefaultMonitoringConfig<E> implements MonitoringConfig<E> {

    private final ArraySet<E> monitoringProbes;

    public DefaultMonitoringConfig(final Class<E> clazz) {
        monitoringProbes = new ArraySet<E>(clazz);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addProbes(final E... probes) {
        monitoringProbes.addAll(probes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean removeProbes(final E... probes) {
        return monitoringProbes.removeAll(probes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final E[] getProbes() {
        return monitoringProbes.obtainArrayCopy();
    }

    /**
     * Get the monitoring probes array (direct).
     *
     * @return the monitoring probes array (direct).
     */
    public final E[] getProbesUnsafe() {
        return monitoringProbes.getArray();
    }

    @Override
    public boolean hasProbes() {
        return !monitoringProbes.isEmpty();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void clearProbes() {
        monitoringProbes.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object createManagementObject() {
        return null;
    }
}
