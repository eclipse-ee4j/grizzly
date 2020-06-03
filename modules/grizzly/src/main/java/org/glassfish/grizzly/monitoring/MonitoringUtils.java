/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;

/**
 * The class, which contains utility methods for monitoring support.
 *
 * @author Alexey Stashok
 */
public class MonitoringUtils {
    private static final Logger LOGGER = Grizzly.logger(MonitoringUtils.class);

    /**
     * Load JMX object class and create an instance using constructor with constructorParam.getClass() parameter. The
     * constructorParam will be passed to the constructor as a parameter.
     *
     * @param jmxObjectClassname the JMX object class name.
     * @param constructorParam the parameter to be passed to the constructor.
     * @return instance of jmxObjectClassname class.
     */
    public static Object loadJmxObject(final String jmxObjectClassname, final Object constructorParam) {
        return loadJmxObject(jmxObjectClassname, constructorParam, constructorParam.getClass());
    }

    /**
     * Load JMX object class and create an instance using constructor with contructorParamType parameter. The
     * constructorParam will be passed to the constructor as a parameter.
     *
     * @param jmxObjectClassname the JMX object class name.
     * @param constructorParam the parameter to be passed to the constructor.
     * @param contructorParamType the constructor parameter type, used to find appropriate constructor.
     * @return instance of jmxObjectClassname class.
     */
    public static Object loadJmxObject(final String jmxObjectClassname, final Object constructorParam, final Class contructorParamType) {
        try {
            final Class<?> clazz = loadClass(jmxObjectClassname);
            final Constructor<?> c = clazz.getDeclaredConstructor(contructorParamType);
            return c.newInstance(constructorParam);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Can not load JMX Object: " + jmxObjectClassname, e);
        }

        return null;
    }

    private static Class<?> loadClass(final String classname) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = MonitoringUtils.class.getClassLoader();
        }

        return classLoader.loadClass(classname);
    }
}
