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

import org.glassfish.grizzly.ConnectionProbe;
import org.glassfish.grizzly.TransportProbe;
import org.glassfish.grizzly.http.HttpProbe;
import org.glassfish.grizzly.http.server.filecache.FileCacheProbe;
import org.glassfish.grizzly.memory.MemoryProbe;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.glassfish.grizzly.monitoring.DefaultMonitoringConfig;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;

/**
 * Grizzly web server monitoring config.
 * 
 * @author Alexey Stashok
 */
public final class HttpServerMonitoringConfig {
    private final DefaultMonitoringConfig<MemoryProbe> memoryConfig =
            new DefaultMonitoringConfig<MemoryProbe>(MemoryProbe.class);

    private final DefaultMonitoringConfig<TransportProbe> transportConfig =
            new DefaultMonitoringConfig<TransportProbe>(TransportProbe.class);

    private final DefaultMonitoringConfig<ConnectionProbe> connectionConfig =
            new DefaultMonitoringConfig<ConnectionProbe>(ConnectionProbe.class);

    private final DefaultMonitoringConfig<ThreadPoolProbe> threadPoolConfig =
            new DefaultMonitoringConfig<ThreadPoolProbe>(ThreadPoolProbe.class);

    private final DefaultMonitoringConfig<FileCacheProbe> fileCacheConfig =
            new DefaultMonitoringConfig<FileCacheProbe>(FileCacheProbe.class);

    private final DefaultMonitoringConfig<HttpProbe> httpConfig =
            new DefaultMonitoringConfig<HttpProbe>(HttpProbe.class);

    private final DefaultMonitoringConfig<HttpServerProbe> webServerConfig =
            new DefaultMonitoringConfig<HttpServerProbe>(HttpServerProbe.class);

    /**
     * Get the memory monitoring config.
     *
     * @return the memory monitoring config.
     */
    public MonitoringConfig<MemoryProbe> getMemoryConfig() {
        return memoryConfig;
    }

    /**
     * Get the connection monitoring config.
     *
     * @return the connection monitoring config.
     */
    public MonitoringConfig<ConnectionProbe> getConnectionConfig() {
        return connectionConfig;
    }

    /**
     * Get the thread pool monitoring config.
     *
     * @return the thread pool monitoring config.
     */
    public MonitoringConfig<ThreadPoolProbe> getThreadPoolConfig() {
        return threadPoolConfig;
    }

    /**
     * Get the transport monitoring config.
     *
     * @return the transport monitoring config.
     */
    public MonitoringConfig<TransportProbe> getTransportConfig() {
        return transportConfig;
    }

    /**
     * Get the file cache monitoring config.
     *
     * @return the file cache monitoring config.
     */
    public MonitoringConfig<FileCacheProbe> getFileCacheConfig() {
        return fileCacheConfig;
    }

    /**
     * Get the http monitoring config.
     *
     * @return the http monitoring config.
     */
    public MonitoringConfig<HttpProbe> getHttpConfig() {
        return httpConfig;
    }

    /**
     * Get the web server monitoring config.
     *
     * @return the web server monitoring config.
     */
    public MonitoringConfig<HttpServerProbe> getWebServerConfig() {
        return webServerConfig;
    }
}
