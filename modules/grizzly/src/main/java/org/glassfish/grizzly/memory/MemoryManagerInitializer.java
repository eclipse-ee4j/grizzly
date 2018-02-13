/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Grizzly;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.grizzly.memory.DefaultMemoryManagerFactory.DMMF_PROP_NAME;

class MemoryManagerInitializer {

    private static final String DMM_PROP_NAME =
            "org.glassfish.grizzly.DEFAULT_MEMORY_MANAGER";

    private static final Logger LOGGER =
            Grizzly.logger(MemoryManagerInitializer.class);


    // ------------------------------------------------- Package-Private Methods


    static MemoryManager initManager() {

        final MemoryManager mm = initMemoryManagerViaFactory();
        return (mm != null) ? mm : initMemoryManagerFallback();

    }


    // --------------------------------------------------------- Private Methods


    @SuppressWarnings("unchecked")
    private static MemoryManager initMemoryManagerViaFactory() {
        String dmmfClassName = System.getProperty(DMMF_PROP_NAME);
        if (dmmfClassName != null) {
            final DefaultMemoryManagerFactory mmf = newInstance(dmmfClassName);
            if (mmf != null) {
                return mmf.createMemoryManager();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static MemoryManager initMemoryManagerFallback() {
        final String className = System.getProperty(DMM_PROP_NAME);
        final MemoryManager mm = newInstance(className);
        return (mm != null) ? mm : new HeapMemoryManager();
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(final String className) {
        if (className == null) {
            return null;
        }
        try {
            Class clazz =
                    Class.forName(className,
                                  true,
                                  MemoryManager.class.getClassLoader());
            return (T) clazz.newInstance();
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.SEVERE)) {
                LOGGER.log(Level.SEVERE,
                           "Unable to load or create a new instance of Class {0}.  Cause: {1}",
                           new Object[]{className, e.getMessage()});
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, e.toString(), e);
            }
            return null;
        }
    }

}
