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

package org.glassfish.grizzly.memory;

/**
 * This interface may be implemented by custom {@link MemoryManager} implementations in order to provide per-thread
 * memory management.
 *
 * When Grizzly managed threads are created, if the {@link MemoryManager} implements this interface,
 * {@link #createThreadLocalPool()} will be invoked and the resulting {@link ThreadLocalPool} will be passed to the
 * {@link Thread}.
 *
 * @since 2.0
 */
public interface ThreadLocalPoolProvider {

    /**
     * @return a new {@link ThreadLocalPool} implementation. This method must return a new {@link ThreadLocalPool} instance
     * per invocation.
     */
    ThreadLocalPool createThreadLocalPool();

}
