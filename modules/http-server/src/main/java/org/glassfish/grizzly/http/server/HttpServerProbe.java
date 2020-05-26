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

package org.glassfish.grizzly.http.server;

import org.glassfish.grizzly.Connection;

/**
 * Monitoring probe providing callbacks that may be invoked by Grizzly {@link HttpServerFilter}.
 *
 * @author Alexey Stashok
 *
 * @since 2.0
 */
public interface HttpServerProbe {
    /**
     * Method will be called, when new {@link Request} will come.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request received {@link Request}.
     */
    void onRequestReceiveEvent(HttpServerFilter filter, Connection connection, Request request);

    /**
     * Method will be called, when {@link Request} processing will be completed.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param response sent {@link Response}.
     */
    void onRequestCompleteEvent(HttpServerFilter filter, Connection connection, Response response);

    /**
     * Method will be called, when {@link Request} processing is suspended.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request {@link Request}.
     */
    void onRequestSuspendEvent(HttpServerFilter filter, Connection connection, Request request);

    /**
     * Method will be called, when {@link Request} processing is resumed.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request {@link Request}.
     */
    void onRequestResumeEvent(HttpServerFilter filter, Connection connection, Request request);

    /**
     * Method will be called, when {@link Request} processing is timeout after suspend.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request {@link Request}.
     */
    void onRequestTimeoutEvent(HttpServerFilter filter, Connection connection, Request request);

    /**
     * Method will be called, when {@link Request} processing is cancelled after suspend.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request {@link Request}.
     */
    void onRequestCancelEvent(HttpServerFilter filter, Connection connection, Request request);

    /**
     * Method will be called, before invoking
     * {@link HttpHandler#service(org.glassfish.grizzly.http.server.Request, org.glassfish.grizzly.http.server.Response)}.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request received {@link Request}.
     * @param httpHandler {@link HttpHandler} to be invoked.
     */
    void onBeforeServiceEvent(HttpServerFilter filter, Connection connection, Request request, HttpHandler httpHandler);

    // ---------------------------------------------------------- Nested Classes

    /**
     * {@link HttpServerProbe} adapter that provides no-op implementations for all interface methods allowing easy extension
     * by the developer.
     *
     * @since 2.1.9
     */
    @SuppressWarnings("UnusedDeclaration")
    class Adapter implements HttpServerProbe {

        // ---------------------------------------- Methods from HttpServerProbe

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestReceiveEvent(HttpServerFilter filter, Connection connection, Request request) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestCompleteEvent(HttpServerFilter filter, Connection connection, Response response) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestSuspendEvent(HttpServerFilter filter, Connection connection, Request request) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestResumeEvent(HttpServerFilter filter, Connection connection, Request request) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRequestTimeoutEvent(HttpServerFilter filter, Connection connection, Request request) {
        }

        @Override
        public void onRequestCancelEvent(HttpServerFilter filter, Connection connection, Request request) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onBeforeServiceEvent(HttpServerFilter filter, Connection connection, Request request, HttpHandler httpHandler) {
        }
    }
}
