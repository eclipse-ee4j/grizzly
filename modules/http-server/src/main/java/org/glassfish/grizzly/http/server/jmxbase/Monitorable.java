/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.jmxbase;

/**
 * Interface that allows arbitrary entities to provide a {@link JmxObject} that can be registered with the
 * {@link org.glassfish.grizzly.monitoring.jmx.GrizzlyJmxManager}.
 */
public interface Monitorable {

    /**
     * Returns a new JMX {@link Object} that may be registered with the Grizzly JMX manager. The returned object has to be
     * of <tt>JmxObject</tt> type, see Grizzly monitoring module for more details.
     */
    Object createManagementObject();

}
