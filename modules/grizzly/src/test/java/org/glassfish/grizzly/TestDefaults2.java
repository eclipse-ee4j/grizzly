/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

import org.glassfish.grizzly.memory.DefaultMemoryManagerFactory;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TestDefaults2 {
    @BeforeClass
    public static void init() {
        System.setProperty(DefaultMemoryManagerFactory.DMMF_PROP_NAME,
                           TestFactory.class.getName());
    }


    // ------------------------------------------------------------ Test Methods


    @Test
    public void testDefaults() throws Exception {

        assertEquals(MemoryManager.DEFAULT_MEMORY_MANAGER.getClass(),
                     TestManager2.class);

    }


    // ---------------------------------------------------------- Nested Classes

    public static final class TestFactory implements DefaultMemoryManagerFactory {

        @Override
        public MemoryManager createMemoryManager() {
            return new TestManager2();
        }
    }

    public static final class TestManager2 implements MemoryManager {

        @Override
        public Buffer allocate(int size) {
            return null;
        }

        @Override
        public Buffer allocateAtLeast(int size) {
            return null;
        }

        @Override
        public Buffer reallocate(Buffer oldBuffer, int newSize) {
            return null;
        }

        @Override
        public void release(Buffer buffer) {
        }

        @Override
        public boolean willAllocateDirect(int size) {
            return false;
        }

        @Override
        public MonitoringConfig getMonitoringConfig() {
            return null;
        }

    } // END TestManager

}
