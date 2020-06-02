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

package org.glassfish.grizzly.jmxbase;

import java.util.Iterator;

import org.glassfish.grizzly.monitoring.MonitoringUtils;
import org.glassfish.grizzly.utils.ServiceFinder;

/**
 * Grizzly JMX manager
 *
 * @author Alexey Stashok
 */
public abstract class GrizzlyJmxManager {
    private static final GrizzlyJmxManager manager;

    static {
        ServiceFinder<GrizzlyJmxManager> serviceFinder = ServiceFinder.find(GrizzlyJmxManager.class);
        final Iterator<GrizzlyJmxManager> it = serviceFinder.iterator();
        GrizzlyJmxManager jmxManager;

        if (it.hasNext()) {
            jmxManager = it.next();
        } else {
            try {
                jmxManager = (GrizzlyJmxManager) loadClass("org.glassfish.grizzly.monitoring.jmx.DefaultJmxManager").newInstance();
            } catch (Exception e) {
                jmxManager = new NullJmxManager();
            }
        }

        manager = jmxManager;
    }

    private static Class<?> loadClass(final String classname) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = MonitoringUtils.class.getClassLoader();
        }

        return classLoader.loadClass(classname);
    }

    /**
     * Return the <tt>GrizzlyJmxManager</tt> instance.
     *
     * @return the <tt>GrizzlyJmxManager</tt> instance.
     */
    public static GrizzlyJmxManager instance() {
        return manager;
    }

    /**
     * Register Grizzly JMX {@link Object} at the root.
     *
     * @param object JMX {@link Object} to register.
     * @return JMX Bean object.
     */
    public abstract Object registerAtRoot(Object object);

    /**
     * Register Grizzly JMX {@link Object} at the root with the passed name.
     *
     * @param object JMX {@link Object} to register.
     * @param name
     * @return JMX Bean object.
     */
    public abstract Object registerAtRoot(Object object, String name);

    /**
     * Register Grizzly JMX {@link Object} as child of the passed parent object.
     *
     * @param parent parent
     * @param object JMX {@link Object} to register.
     * @return JMX Bean object.
     */
    public abstract Object register(Object parent, Object object);

    /**
     * Register Grizzly JMX {@link Object} as child of the passed parent object with the specific name.
     *
     * @param parent parent
     * @param object JMX {@link Object} to register.
     * @param name
     * @return JMX Bean object.
     */
    public abstract Object register(Object parent, Object object, String name);

    /**
     * Unregister Grizzly JMX {@link Object}.
     *
     * @param object JMX {@link Object} to deregister.
     */
    public abstract void deregister(Object object);

    /**
     * Null JMX manager
     */
    private static final class NullJmxManager extends GrizzlyJmxManager {

        @Override
        public Object registerAtRoot(Object object) {
            return null;
        }

        @Override
        public Object registerAtRoot(Object object, String name) {
            return null;
        }

        @Override
        public Object register(Object parent, Object object) {
            return null;
        }

        @Override
        public Object register(Object parent, Object object, String name) {
            return null;
        }

        @Override
        public void deregister(Object object) {
        }
    }
}
