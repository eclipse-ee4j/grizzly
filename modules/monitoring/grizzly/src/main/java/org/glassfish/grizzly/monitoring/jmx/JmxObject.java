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

package org.glassfish.grizzly.monitoring.jmx;

import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * Class represents any kind of JMX object in Grizzly. All the abstractions in
 * Grizzly, which have to be exposed via JMX, should extend this class.
 * 
 * @author Alexey Stashok
 */
public abstract class JmxObject {

    /**
     * @return the name this managed object should be registered with.
     */
    public abstract String getJmxName();

    /**
     * Method will be called right after this <tt>JmxObject</tt> is registered by the JMX manager.
     * 
     * @param mom {@link GrizzlyJmxManager} Grizzly JMX manager.
     * @param bean {@link GmbalMBean}, which represents the registration.
     */
    protected abstract void onRegister(GrizzlyJmxManager mom, GmbalMBean bean);

    /**
     * Method will be called right after this <tt>JmxObject</tt> is unregistered by the JMX manager.
     *
     * @param mom {@link GrizzlyJmxManager} Grizzly JMX manager.
     */
    protected abstract void onDeregister(GrizzlyJmxManager mom);
}
