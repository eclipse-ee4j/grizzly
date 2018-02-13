/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.osgi.httpservice;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Abstract OSGi {@link org.glassfish.grizzly.http.server.HttpHandler}.
 * <p/>
 * Defines locking model for processing and removal of handler.
 *
 * @author Hubert Iwaniuk
 */
public interface OSGiHandler {
    /**
     * Processing lock.
     * <p/>
     * {@link ReentrantReadWriteLock.ReadLock} can be obtained by concurrent threads at the same time.
     *
     * @return Processing lock.
     */
    ReentrantReadWriteLock.ReadLock getProcessingLock();

    /**
     * Removal lock.
     * <p/>
     * {@link ReentrantReadWriteLock.WriteLock} can be obtained only by one thread, blocks {@link #getProcessingLock()}.
     *
     * @return Removal lock.
     * @see #getProcessingLock()
     * @see ReentrantReadWriteLock.WriteLock
     */
    ReentrantReadWriteLock.WriteLock getRemovalLock();
}
