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

package org.glassfish.grizzly;

/**
 * Interface to notify interested parties that a {@link Transport} is being shutdown.
 *
 * Keep in mind that there is no guarantee that all listeners will be invoked before the transport is terminated (e.g.,
 * timed graceful shutdown or a graceful shutdown() that was initiated and then shutdownNow() is later invoked.
 *
 * @since 2.3.5.
 */
public interface GracefulShutdownListener {

    /**
     * Invoked when an attempt is made to shutdown the transport gracefully.
     *
     * @param shutdownContext the {@link ShutdownContext} for this shutdown request.
     */
    void shutdownRequested(final ShutdownContext shutdownContext);

    /**
     * Invoked when the transport is being shutdown forcefully. This means either shutdownNow() was invoked or the graceful
     * shutdown timed out. It's important that the implementation of this method not block.
     */
    void shutdownForced();

}
