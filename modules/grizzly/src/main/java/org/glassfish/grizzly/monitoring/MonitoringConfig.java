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

/**
 * General monitoring configuration interface.
 *
 * @author Alexey Stashok
 */
public interface MonitoringConfig<E> {
    /**
     * Add the monitoring probes, which will be notified about object's lifecycle events.
     *
     * @param probes the monitoring probes.
     */
    void addProbes(E... probes);

    /**
     * Remove the monitoring probes.
     *
     * @param probes the monitoring probes.
     */
    boolean removeProbes(E... probes);

    /**
     * Get the the monitoring probes, which are registered on the objet.
     * Please note, it's not appropriate to modify the returned array's content.
     * Please use {@link #addProbes(Object[])} and
     * {@link #removeProbes(Object[])} instead.
     *
     * @return the the monitoring probes, which are registered on the object.
     */
    E[] getProbes();

    boolean hasProbes();
    
    /**
     * Removes all the monitoring probes, which are registered on the object.
     */
    void clearProbes();
    
    /**
     * Create the JMX {@link Object}, which represents this object.
     * 
     * @return the JMX {@link Object}, which represents this object.
     */
    Object createManagementObject();
}
