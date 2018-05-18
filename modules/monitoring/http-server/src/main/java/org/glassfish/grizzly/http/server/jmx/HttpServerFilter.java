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

package org.glassfish.grizzly.http.server.jmx;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.HttpServerProbe;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * JMX management object for the {@link org.glassfish.grizzly.http.server.HttpServerFilter}.
 *
 * @since 2.0
 */
@ManagedObject
@Description("The HttpServerFilter is the entity responsible for providing and processing higher level abstractions based on HTTP protocol.")
public class HttpServerFilter extends JmxObject {

    private final org.glassfish.grizzly.http.server.HttpServerFilter httpServerFilter;

    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong completedCount = new AtomicLong();
    private final AtomicInteger suspendCount = new AtomicInteger();
    private final AtomicLong timedOutCount = new AtomicLong();
    private final AtomicLong cancelledCount = new AtomicLong();

    private final HttpServerProbe probe = new JmxWebServerProbe();

    // ------------------------------------------------------------ Constructors


    public HttpServerFilter(org.glassfish.grizzly.http.server.HttpServerFilter httpServerFilter) {
        this.httpServerFilter = httpServerFilter;
    }


    // -------------------------------------------------- Methods from JmxObject


    @Override
    public String getJmxName() {
        return "HttpServerFilter";
    }

    @Override
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
        httpServerFilter.getMonitoringConfig().addProbes(probe);
    }

    @Override
    protected void onDeregister(GrizzlyJmxManager mom) {
        httpServerFilter.getMonitoringConfig().removeProbes(probe);
    }


    // -------------------------------------------------------------- Attributes


    /**
     * @return the number of requests this {@link org.glassfish.grizzly.http.server.HttpServerFilter}
     *  has received.
     */
    @ManagedAttribute(id="requests-received-count")
    @Description("The total number of requests received.")
    public long getRequestsReceivedCount() {
        return receivedCount.get();
    }


    /**
     * @return the number of requests this {@link org.glassfish.grizzly.http.server.HttpServerFilter}
     *  has completed servicing.
     */
    @ManagedAttribute(id="requests-completed-count")
    @Description("The total number of requests that have been successfully completed.")
    public long getRequestsCompletedCount() {
        return completedCount.get();
    }


    /**
     * @return the number of requests currently suspended.
     */
    @ManagedAttribute(id="current-suspended-request-count")
    @Description("The current number of requests that are suspended to be processed at a later point in time.")
    public int getRequestsSuspendedCount() {
        return suspendCount.get();
    }


    /**
     * @return the number of suspended requests that have timed out.
     */
    @ManagedAttribute(id="requests-timed-out-count")
    @Description("The total number of suspended requests that have been timed out.")
    public long getRequestsTimedOutCount() {
        return timedOutCount.get();
    }


    /**
     * @return the number of requests suspended requests that have been
     *  cancelled.
     */
    @ManagedAttribute(id="requests-cancelled-count")
    @Description("The total number of suspended requests that have been cancelled.")
    public long getRequestsCancelledCount() {
        return cancelledCount.get();
    }


    // ---------------------------------------------------------- Nested Classes


    private final class JmxWebServerProbe extends HttpServerProbe.Adapter {


        // ----------------------------------------- Methods from HttpServerProbe


        @Override
        public void onRequestReceiveEvent(org.glassfish.grizzly.http.server.HttpServerFilter filter, Connection connection, Request request) {
            receivedCount.incrementAndGet();
        }

        @Override
        public void onRequestCompleteEvent(org.glassfish.grizzly.http.server.HttpServerFilter filter, Connection connection, Response response) {
            completedCount.incrementAndGet();
        }

        @Override
        public void onRequestSuspendEvent(org.glassfish.grizzly.http.server.HttpServerFilter filter, Connection connection, Request request) {
            suspendCount.incrementAndGet();
        }

        @Override
        public void onRequestResumeEvent(org.glassfish.grizzly.http.server.HttpServerFilter filter, Connection connection, Request request) {
            if (suspendCount.get() > 0) {
                suspendCount.decrementAndGet();
            }
        }

        @Override
        public void onRequestTimeoutEvent(org.glassfish.grizzly.http.server.HttpServerFilter filter, Connection connection, Request request) {
            timedOutCount.incrementAndGet();
            if (suspendCount.get() > 0) {
                suspendCount.decrementAndGet();
            }
        }

        @Override
        public void onRequestCancelEvent(org.glassfish.grizzly.http.server.HttpServerFilter filter, Connection connection, Request request) {
            cancelledCount.incrementAndGet();
            if (suspendCount.get() > 0) {
                suspendCount.decrementAndGet();
            }
        }

    } // END JmxWebServerProbe
    
}
