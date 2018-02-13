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

package org.glassfish.grizzly.http.server;

import org.glassfish.grizzly.Connection;

/**
 * Utility class, which has notification methods for different
 * {@link HttpServerProbe} events.
 *
 * @author Alexey Stashok
 */
final class HttpServerProbeNotifier {
    /**
     * Notify registered {@link HttpServerProbe}s about the "request received" event.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request received {@link Request}.
     */
    static void notifyRequestReceive(final HttpServerFilter filter,
            final Connection connection, final Request request) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onRequestReceiveEvent(filter, connection, request);
            }
        }
    }

    /**
     * Notify registered {@link HttpServerProbe}s about the "request completed" event.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param response {@link Response}.
     */
    static void notifyRequestComplete(final HttpServerFilter filter,
            final Connection connection, final Response response) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onRequestCompleteEvent(filter, connection, response);
            }
        }
    }

    /**
     * Notify registered {@link HttpServerProbe}s about the "request suspended" event.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request {@link Request}.
     */
    static void notifyRequestSuspend(final HttpServerFilter filter,
            final Connection connection, final Request request) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onRequestSuspendEvent(filter, connection, request);
            }
        }
    }

    /**
     * Notify registered {@link HttpServerProbe}s about the "request resumed" event.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request {@link Request}.
     */
    static void notifyRequestResume(final HttpServerFilter filter,
            final Connection connection, final Request request) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onRequestResumeEvent(filter, connection, request);
            }
        }
    }

    /**
     * Notify registered {@link HttpServerProbe}s about the "request timeout after suspend" event.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request  {@link Request}.
     */
    static void notifyRequestTimeout(final HttpServerFilter filter,
            final Connection connection, final Request request) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onRequestTimeoutEvent(filter, connection, request);
            }
        }
    }

    /**
     * Notify registered {@link HttpServerProbe}s about the "request canceled after suspend" event.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param request  {@link Request}.
     */
    static void notifyRequestCancel(final HttpServerFilter filter,
            final Connection connection, final Request request) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onRequestCancelEvent(filter, connection, request);
            }
        }
    }
    
    /**
     * Notify registered {@link HttpServerProbe}s before invoking
     * {@link HttpHandler#service(org.glassfish.grizzly.http.server.Request, org.glassfish.grizzly.http.server.Response)}.
     *
     * @param filter {@link HttpServerFilter}, the event belongs to.
     * @param connection {@link Connection}, the event belongs to.
     * @param response {@link Response}.
     * @param httpHandler {@link HttpHandler}.
     */
    static void notifyBeforeService(final HttpServerFilter filter,
            final Connection connection, final Request request,
            final HttpHandler httpHandler) {

        final HttpServerProbe[] probes = filter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpServerProbe probe : probes) {
                probe.onBeforeServiceEvent(filter, connection, request, httpHandler);
            }
        }
    }    
}
