/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.Connection;

/**
 * Monitoring probe providing callbacks that may be invoked by Grizzly {@link KeepAlive}.
 *
 * @author Alexey Stashok
 *
 * @since 2.0
 */
public interface KeepAliveProbe {

    /**
     * Method will be called, when new keep-alive HTTP connection is getting established.
     * This method is getting invoked, when 1st HTTP request processing completes,
     * but the Connection will be kept alive to process next HTTP request.
     *
     * @param connection {@link Connection}, the event belongs to.
     */
    void onConnectionAcceptEvent(Connection connection);

    /**
     * Method will be called, when HTTP request comes on a kept alive connection.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param requestNumber HTTP request number, being processed on the given keep-alive connection.
     */
    void onHitEvent(Connection connection, int requestNumber);

    /**
     * Method will be called, when the Connection could be used in the keep alive mode,
     * but due to KeepAlive config limitations it will be closed.
     *
     * @param connection {@link Connection}, the event belongs to.
     */
    void onRefuseEvent(Connection connection);

    /**
     * Method will be called, when the keep alive Connection idle timeout expired.
     *
     * @param connection {@link Connection}, the event belongs to.
     */
    void onTimeoutEvent(Connection connection);


    // ---------------------------------------------------------- Nested Classes


    /**
     * {@link KeepAliveProbe} adapter that provides no-op implementations for
     * all interface methods allowing easy extension by the developer.
     *
     * @since 2.1.9
     */
    @SuppressWarnings("UnusedDeclaration")
    class Adapter implements KeepAliveProbe {


        // ----------------------------------------- Methods from KeepAliveProbe

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnectionAcceptEvent(Connection connection) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onHitEvent(Connection connection, int requestNumber) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRefuseEvent(Connection connection) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTimeoutEvent(Connection connection) {}

    } // END Adapter
}
