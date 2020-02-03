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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.util.Constants;
import org.glassfish.grizzly.monitoring.MonitoringAware;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.glassfish.grizzly.monitoring.DefaultMonitoringConfig;
import org.glassfish.grizzly.monitoring.MonitoringUtils;

/**
 * Web container configuration for keep-alive HTTP connections.
 * 
 * @author Alexey Stashok
 */
public final class KeepAlive implements MonitoringAware<KeepAliveProbe> {
    /**
     * Keep alive probes
     */
    protected final DefaultMonitoringConfig<KeepAliveProbe> monitoringConfig;
    
    /**
     * The number int seconds a connection may be idle before being timed out.
     */
    private int idleTimeoutInSeconds = Constants.KEEP_ALIVE_TIMEOUT_IN_SECONDS;

    /**
     * The max number of HTTP requests allowed to be processed on one keep-alive connection.
     */
    private int maxRequestsCount = Constants.DEFAULT_MAX_KEEP_ALIVE;

    public KeepAlive() {
        monitoringConfig = new DefaultMonitoringConfig<KeepAliveProbe>(KeepAliveProbe.class) {

            @Override
            public Object createManagementObject() {
                return createJmxManagementObject();
            }

        };
    }

    /**
     * The copy constructor.
     * @param keepAlive the {@link KeepAlive} to copy
     */
    public KeepAlive(final KeepAlive keepAlive) {
        this.monitoringConfig = keepAlive.monitoringConfig;
        this.idleTimeoutInSeconds = keepAlive.idleTimeoutInSeconds;
        this.maxRequestsCount = keepAlive.maxRequestsCount;
    }


    /**
     * @return the number in seconds a connection may be idle before being
     *  timed out.
     */
    public int getIdleTimeoutInSeconds() {

        return idleTimeoutInSeconds;

    }


    /**
     * <p>
     * Configures idle connection timeout behavior.
     * </p>
     *
     * @param idleTimeoutInSeconds the number in seconds a connection may
     *  be idle before being timed out.  Values less than zero are considered as FOREVER.
     */
    public void setIdleTimeoutInSeconds(final int idleTimeoutInSeconds) {

        if (idleTimeoutInSeconds < 0) {
            this.idleTimeoutInSeconds = -1;
        } else {
            this.idleTimeoutInSeconds = idleTimeoutInSeconds;
        }

    }

    /**
     * @return the max number of HTTP requests allowed to be processed on one keep-alive connection.
     */
    public int getMaxRequestsCount() {
        return maxRequestsCount;
    }

    /**
     * <p>
     * Configures the max number of HTTP requests allowed to be processed on one keep-alive connection.
     * </p>
     *
     * @param maxRequestsCount the max number of HTTP requests allowed to be
     * processed on one keep-alive connection. Values less than zero are considered as UNLIMITED.
     */
    public void setMaxRequestsCount(int maxRequestsCount) {
        this.maxRequestsCount = maxRequestsCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitoringConfig<KeepAliveProbe> getMonitoringConfig() {
        return monitoringConfig;
    }

    protected Object createJmxManagementObject() {
        return MonitoringUtils.loadJmxObject(
                "org.glassfish.grizzly.http.jmx.KeepAlive", this, KeepAlive.class);
    }

    /**
     * Notify registered {@link KeepAliveProbe}s about the "keep-alive connection accepted" event.
     *
     * @param keepAlive the <tt>KeepAlive</tt> event occurred on.
     * @param connection {@link Connection} been accepted.
     */
    protected static void notifyProbesConnectionAccepted(
            final KeepAlive keepAlive, final Connection connection) {
        final KeepAliveProbe[] probes =
                keepAlive.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (KeepAliveProbe probe : probes) {
                probe.onConnectionAcceptEvent(connection);
            }
        }
    }

    /**
     * Notify registered {@link KeepAliveProbe}s about the "keep-alive connection hit" event.
     *
     * @param keepAlive the <tt>KeepAlive</tt> event occurred on.
     * @param connection {@link Connection} been hit.
     * @param requestNumber the request number being processed on the given {@link Connection}.
     */
    protected static void notifyProbesHit(
            final KeepAlive keepAlive, final Connection connection,
            final int requestNumber) {
        
        final KeepAliveProbe[] probes =
                keepAlive.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (KeepAliveProbe probe : probes) {
                probe.onHitEvent(connection, requestNumber);
            }
        }
    }

    /**
     * Notify registered {@link KeepAliveProbe}s about the "keep-alive connection refused" event.
     *
     * @param keepAlive the <tt>KeepAlive</tt> event occurred on.
     * @param connection {@link Connection} been refused.
     */
    protected static void notifyProbesRefused(
            final KeepAlive keepAlive, final Connection connection) {

        final KeepAliveProbe[] probes =
                keepAlive.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (KeepAliveProbe probe : probes) {
                probe.onRefuseEvent(connection);
            }
        }
    }

    /**
     * Notify registered {@link KeepAliveProbe}s about the "keep-alive connection timeout" event.
     *
     * @param keepAlive the <tt>KeepAlive</tt> event occurred on.
     * @param connection {@link Connection} been timeout.
     */
    protected static void notifyProbesTimeout(
            final KeepAlive keepAlive, final Connection connection) {

        final KeepAliveProbe[] probes =
                keepAlive.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (KeepAliveProbe probe : probes) {
                probe.onTimeoutEvent(connection);
            }
        }
    }

}
