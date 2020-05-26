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
 * Monitoring probe providing callbacks that may be invoked by Grizzly {@link Transport} implementations.
 *
 * @author Alexey Stashok
 *
 * @since 2.0
 */
public interface TransportProbe {

    /**
     * Method will be called before starting the {@link Transport}.
     *
     * @param transport {@link Transport}, the event belongs to.
     *
     * @since 3.0
     */
    void onBeforeStartEvent(Transport transport);

    /**
     * Method will be called when the {@link Transport} has been started.
     *
     * @param transport {@link Transport}, the event belongs to.
     */
    void onStartEvent(Transport transport);

    /**
     * Method will be called before stopping the {@link Transport}.
     *
     * @param transport {@link Transport}, the event belongs to.
     *
     * @since 3.0
     */
    void onBeforeStopEvent(Transport transport);

    /**
     * Method will be called when the {@link Transport} has been stopped.
     *
     * @param transport {@link Transport}, the event belongs to.
     */
    void onStopEvent(Transport transport);

    /**
     * Method will be called before pausing the {@link Transport}.
     *
     * @param transport {@link Transport}, the event belongs to.
     *
     * @since 3.0
     */
    void onBeforePauseEvent(Transport transport);

    /**
     * Method will be called when the {@link Transport} is paused.
     *
     * @param transport {@link Transport}, the event belongs to.
     */
    void onPauseEvent(Transport transport);

    /**
     * Method will be called before resuming the {@link Transport}.
     *
     * @param transport {@link Transport}, the event belongs to.
     *
     * @since 3.0
     */
    void onBeforeResumeEvent(Transport transport);

    /**
     * Method will be called, when the {@link Transport} gets resumed.
     *
     * @param transport {@link Transport}, the event belongs to.
     */
    void onResumeEvent(Transport transport);

    /**
     * Method will be called, when the {@link Transport} configuration gets changed.
     *
     * @param transport {@link Transport}, the event belongs to.
     */
    void onConfigChangeEvent(Transport transport);

    /**
     * Method will be called, when error occurs on the {@link Transport}.
     *
     * @param transport {@link Transport}, the event belongs to.
     * @param error error
     */
    void onErrorEvent(Transport transport, Throwable error);

    // ---------------------------------------------------------- Nested Classes

    /**
     * {@link TransportProbe} adapter that provides no-op implementations for all interface methods allowing easy extension
     * by the developer.
     *
     * @since 2.1.9
     */
    @SuppressWarnings("UnusedDeclaration")
    class Adapter implements TransportProbe {

        // ----------------------------------------- Methods from TransportProbe

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeforeStartEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStartEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeforeStopEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onStopEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeforePauseEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onPauseEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeforeResumeEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onResumeEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onConfigChangeEvent(Transport transport) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onErrorEvent(Transport transport, Throwable error) {
        }

    } // END Adapter

}
