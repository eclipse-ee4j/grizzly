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

package org.glassfish.grizzly.memory;

/**
 * Allows creation/customization of {@link MemoryManager} implementation to
 * be used as the {@link MemoryManager#DEFAULT_MEMORY_MANAGER}.
 *
 * Specify a custom <code>DefaultMemoryManagerFactory</code> implementation using
 * a JVM system property {@value #DMMF_PROP_NAME}.  Implementations of this interface
 * will be created by invoking its no-arg constructor.
 *
 * @since 3.0
 */
public interface DefaultMemoryManagerFactory {

    String DMMF_PROP_NAME = "org.glassfish.grizzly.MEMORY_MANAGER_FACTORY";

    /**
     * @return the MemoryManager that should be used as the value of
     * {@link MemoryManager#DEFAULT_MEMORY_MANAGER}.
     * This method should return <code>null</code> if the {@link MemoryManager}
     * could not be created.
     */
    MemoryManager createMemoryManager();

}
