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

package org.glassfish.grizzly.http.jmx;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GenericCloseListener;
import org.glassfish.grizzly.http.KeepAliveProbe;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * JMX management object for {@link org.glassfish.grizzly.http.KeepAlive}.
 *
 * @since 2.0
 */
@ManagedObject
@Description("The configuration for HTTP keep-alive connections.")
public class KeepAlive extends JmxObject {
    /**
     * The {@link org.glassfish.grizzly.http.KeepAlive} being managed.
     */
    private final org.glassfish.grizzly.http.KeepAlive keepAlive;

    /**
     * The number of live keep-alive connections.
     */
    private final AtomicInteger keepAliveConnectionsCount = new AtomicInteger();

    /**
     * The number of requests processed on a keep-alive connections.
     */
    private final AtomicInteger keepAliveHitsCount = new AtomicInteger();

    /**
     * The number of times keep-alive mode was refused.
     */
    private final AtomicInteger keepAliveRefusesCount = new AtomicInteger();

    /**
     * The number of times idle keep-alive connections were closed by timeout.
     */
    private final AtomicInteger keepAliveTimeoutsCount = new AtomicInteger();

    /**
     * The {@link JMXKeepAliveProbe} used to track keep-alive statistics.
     */
    private final JMXKeepAliveProbe keepAliveProbe = new JMXKeepAliveProbe();
    
    // ------------------------------------------------------------ Constructors


    /**
     * Constructs a new JMX managed KeepAlive for the specified
     * {@link org.glassfish.grizzly.http.KeepAlive} instance.
     *
     * @param keepAlive the {@link org.glassfish.grizzly.http.KeepAlive}
     *  to manage.
     */
    public KeepAlive(org.glassfish.grizzly.http.KeepAlive keepAlive) {
        this.keepAlive = keepAlive;
    }

    // -------------------------------------------------- Methods from JmxObject


    /**
     * {@inheritDoc}
     */
    @Override
    public String getJmxName() {
        return "Keep-Alive";
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * <p>
     * When invoked, this method will add a {@link KeepAliveProbe} to track
     * statistics.
     * </p>
     */
    @Override
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
        keepAlive.getMonitoringConfig().addProbes(keepAliveProbe);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * <p>
     * When invoked, this method will remove the {@link KeepAliveProbe} added
     * by the {@link #onRegister(GrizzlyJmxManager, GmbalMBean)}
     * call.
     * </p>
     */
    @Override
    protected void onDeregister(GrizzlyJmxManager mom) {
        keepAlive.getMonitoringConfig().removeProbes(keepAliveProbe);
    }

    // --------------------------------------------------- Keep Alive Properties


    /**
     * @see org.glassfish.grizzly.http.KeepAlive#getIdleTimeoutInSeconds()
     */
    @ManagedAttribute(id="idle-timeout-seconds")
    @Description("The time period keep-alive connection may stay idle")
    public int getIdleTimeoutInSeconds() {
        return keepAlive.getIdleTimeoutInSeconds();
    }

    /**
     * @see org.glassfish.grizzly.http.KeepAlive#getMaxRequestsCount()
     */
    @ManagedAttribute(id="max-requests-count")
    @Description("the max number of HTTP requests allowed to be processed on one keep-alive connection")
    public int getMaxRequestsCount() {
        return keepAlive.getMaxRequestsCount();
    }

    /**
     * @return the number live keep-alive connections.
     */
    @ManagedAttribute(id="live-connections-count")
    @Description("The number of live keep-alive connections")
    public int getConnectionsCount() {
        return keepAliveConnectionsCount.get();
    }

    /**
     * @return the number of requests processed on a keep-alive connections.
     */
    @ManagedAttribute(id="hits-count")
    @Description("The number of requests processed on a keep-alive connections.")
    public int getHitsCount() {
        return keepAliveHitsCount.get();
    }

    /**
     * @return the number of times keep-alive mode was refused.
     */
    @ManagedAttribute(id="refuses-count")
    @Description("The number of times keep-alive mode was refused.")
    public int getRefusesCount() {
        return keepAliveRefusesCount.get();
    }

    /**
     * @return the number of times idle keep-alive connections were closed by timeout.
     */
    @ManagedAttribute(id="timeouts-count")
    @Description("The number of times idle keep-alive connections were closed by timeout.")
    public int getTimeoutsCount() {
        return keepAliveTimeoutsCount.get();
    }

    // ---------------------------------------------------------- Nested Classes


    /**
     * JMX statistic gathering {@link KeepAliveProbe}.
     */
    private final class JMXKeepAliveProbe implements KeepAliveProbe {

        @Override
        public void onConnectionAcceptEvent(Connection connection) {
            keepAliveConnectionsCount.incrementAndGet();
            connection.addCloseListener(new GenericCloseListener() {

                @Override
                public void onClosed(final Closeable closeable,
                        final CloseType closeType) throws IOException {
                    keepAliveConnectionsCount.decrementAndGet();
                }
            });
        }

        @Override
        public void onHitEvent(Connection connection, int requestCounter) {
            keepAliveHitsCount.incrementAndGet();
        }

        @Override
        public void onRefuseEvent(Connection connection) {
            keepAliveRefusesCount.incrementAndGet();
        }

        @Override
        public void onTimeoutEvent(Connection connection) {
            keepAliveTimeoutsCount.incrementAndGet();
        }


        // ----------------------------------------- Methods from KeepAliveProbe


    } // END JMXKeepAliveProbe
}
