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
import org.glassfish.gmbal.ManagedObjectManager;
import org.glassfish.gmbal.ManagedObjectManagerFactory;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * Grizzly JMX manager
 * 
 * @author Alexey Stashok
 */
public class DefaultJmxManager extends GrizzlyJmxManager {
    protected final ManagedObjectManager mom;

    public DefaultJmxManager() {
        this.mom = ManagedObjectManagerFactory.createStandalone("org.glassfish.grizzly");
        mom.stripPackagePrefix();
        mom.createRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object registerAtRoot(Object object) {
        final String jmxName = object instanceof JmxObject
                ? ((JmxObject) object).getJmxName()
                : identityToString(object);
        
        return registerAtRoot(object, jmxName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GmbalMBean registerAtRoot(Object object, String name) {
        final GmbalMBean bean = mom.registerAtRoot(object, name);
        if (object instanceof JmxObject) {
            ((JmxObject) object).onRegister(this, bean);
        }
        
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object register(Object parent, Object object) {
        final String jmxName = object instanceof JmxObject
                ? ((JmxObject) object).getJmxName()
                : identityToString(object);
        
        return register(parent, object, jmxName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GmbalMBean register(Object parent, Object object, String name) {
        final GmbalMBean bean = mom.register(parent, object, name);
        if (object instanceof JmxObject) {
            ((JmxObject) object).onRegister(this, bean);
        }

        return bean;
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public void deregister(Object object) {
        mom.unregister(object);
        
        if (object instanceof JmxObject) {
            ((JmxObject) object).onDeregister(this);
        }
    }

    protected String identityToString(Object object) {
        return object.getClass().getName() + "@" +
                Integer.toHexString(System.identityHashCode(object));
    }
}
