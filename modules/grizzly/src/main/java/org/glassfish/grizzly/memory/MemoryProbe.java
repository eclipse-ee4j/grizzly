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

package org.glassfish.grizzly.memory;

/**
 * {@link MemoryManager} monitoring probe.
 * 
 * @author Alexey Stashok
 */
public interface MemoryProbe {
    /**
     * Called by {@link MemoryManager}, when new buffer gets allocated
     * 
     * @param size buffer size
     */
    void onBufferAllocateEvent(int size);

    /**
     * Called by {@link MemoryManager}, when buffer gets allocated from some pool
     *
     * @param size buffer size
     */
    void onBufferAllocateFromPoolEvent(int size);

    /**
     * Called by {@link MemoryManager}, when buffer gets released into a buffer pool
     *
     * @param size buffer size
     */
    void onBufferReleaseToPoolEvent(int size);


    // ---------------------------------------------------------- Nested Classes

    /**
     * {@link MemoryProbe} adapter that provides no-op implementations for
     * all interface methods allowing easy extension by the developer.
     *
     * @since 2.1.9
     */
    @SuppressWarnings("UnusedDeclaration")
    class Adapter implements MemoryProbe {


        // -------------------------------------------- Methods from MemoryProbe

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBufferAllocateEvent(int size) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBufferAllocateFromPoolEvent(int size) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBufferReleaseToPoolEvent(int size) {}

    } // END Adapter
}
