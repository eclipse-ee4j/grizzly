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

package org.glassfish.grizzly;

/**
 * Monitoring probe providing callbacks that may be invoked by Grizzly {@link Connection} implementations.
 *
 * @author Alexey Stashok
 *
 * @since 2.0
 */
public interface ConnectionProbe {

    /**
     * Method will be called, when server side connection gets bound.
     *
     * @param connection {@link Connection}, the event belongs to.
     */
    void onBindEvent(Connection connection);

    /**
     * Method will be called, when server side connection gets accepted.
     *
     * @param serverConnection server {@link Connection}, the event belongs to.
     * @param clientConnection new client {@link Connection}.
     */
    void onAcceptEvent(Connection serverConnection, Connection clientConnection);

    /**
     * Method will be called, when client side connection gets connected (opened).
     *
     * @param connection {@link Connection}, the event belongs to.
     */
    void onConnectEvent(Connection connection);

    /**
     * Method will be called, when the {@link Connection} has read data.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param data {@link Buffer}, where the data gets read.
     * @param size the data size.
     */
    void onReadEvent(Connection connection, Buffer data, int size);

    /**
     * Method will be called, when the {@link Connection} has written data.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param data {@link Buffer}, where the data gets writen.
     * @param size the data size.
     */
    void onWriteEvent(Connection connection, Buffer data, long size);

    /**
     * Method will be called, when error occurs on the {@link Connection}.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param error error
     */
    void onErrorEvent(Connection connection, Throwable error);

    /**
     * Method will be called, when {@link Connection} gets closed.
     *
     * @param connection {@link Connection}, the event belongs to.
     */
    void onCloseEvent(Connection connection);

    /**
     * Method will be called, when {@link IOEvent} for the specific {@link Connection} gets ready.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param ioEvent {@link IOEvent}.
     */
    void onIOEventReadyEvent(Connection connection, IOEvent ioEvent);

    /**
     * Method will be called, when {@link IOEvent} for the specific {@link Connection} gets enabled.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param ioEvent {@link IOEvent}.
     */
    void onIOEventEnableEvent(Connection connection, IOEvent ioEvent);

    /**
     * Method will be called, when {@link IOEvent} for the specific {@link Connection} gets disabled.
     *
     * @param connection {@link Connection}, the event belongs to.
     * @param ioEvent {@link IOEvent}.
     */
    void onIOEventDisableEvent(Connection connection, IOEvent ioEvent);

    // ---------------------------------------------------------- Nested Classes

    /**
     * {@link ConnectionProbe} adapter that provides no-op implementations for all interface methods allowing easy extension
     * by the developer.
     *
     * @since 2.1.9
     */
    @SuppressWarnings("UnusedDeclaration")
    class Adapter implements ConnectionProbe {

        // ---------------------------------------- Methods from ConnectionProbe

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBindEvent(Connection connection) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAcceptEvent(Connection serverConnection, Connection clientConnection) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConnectEvent(Connection connection) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReadEvent(Connection connection, Buffer data, int size) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onWriteEvent(Connection connection, Buffer data, long size) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onErrorEvent(Connection connection, Throwable error) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCloseEvent(Connection connection) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onIOEventReadyEvent(Connection connection, IOEvent ioEvent) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onIOEventEnableEvent(Connection connection, IOEvent ioEvent) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onIOEventDisableEvent(Connection connection, IOEvent ioEvent) {
        }

    } // END Adapter

}
