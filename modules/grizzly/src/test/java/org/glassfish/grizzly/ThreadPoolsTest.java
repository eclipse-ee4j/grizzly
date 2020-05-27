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

import static junit.framework.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.strategies.SameThreadIOStrategy;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;
import org.glassfish.grizzly.threadpool.AbstractThreadPool;
import org.glassfish.grizzly.threadpool.FixedThreadPool;
import org.glassfish.grizzly.threadpool.SyncThreadPool;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.junit.Test;

public class ThreadPoolsTest {

    /**
     * Added for http://java.net/jira/browse/GRIZZLY-1435.
     * 
     * @throws Exception
     */
    @Test
    public void testThreadPoolCoreThreadInitialization() throws Exception {
        final ThreadPoolConfig config = ThreadPoolConfig.defaultConfig();
        config.setCorePoolSize(5);
        config.setMaxPoolSize(5);
        Field workers = AbstractThreadPool.class.getDeclaredField("workers");
        workers.setAccessible(true);

        final SyncThreadPool syncThreadPool = new SyncThreadPool(config);
        assertEquals("Pool did not properly initialize threads based on core pool size configuration.", 5, ((Map) workers.get(syncThreadPool)).size());

        config.setQueue(new ArrayBlockingQueue<Runnable>(5));
        final FixedThreadPool fixedThreadPool = new FixedThreadPool(config);
        assertEquals("Pool did not properly initialize threads based on core pool size configuration.", 5, ((Map) workers.get(fixedThreadPool)).size());
    }

    @Test
    public void testCustomThreadPoolSameThreadStrategy() throws Exception {

        final int poolSize = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        final ThreadPoolConfig poolCfg = ThreadPoolConfig.defaultConfig();
        poolCfg.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);

        final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder.newInstance().setReuseAddress(true).setIOStrategy(SameThreadIOStrategy.getInstance())
                .setSelectorThreadPoolConfig(poolCfg).setWorkerThreadPoolConfig(null).build();
        try {
            tcpTransport.start();
        } finally {
            tcpTransport.shutdownNow();
        }
    }

    @Test
    public void testCustomThreadPoolWorkerThreadStrategy() throws Exception {

        final int selectorPoolSize = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);
        final ThreadPoolConfig selectorPoolCfg = ThreadPoolConfig.defaultConfig();
        selectorPoolCfg.setCorePoolSize(selectorPoolSize).setMaxPoolSize(selectorPoolSize);

        final int workerPoolSize = Runtime.getRuntime().availableProcessors() * 2;
        final ThreadPoolConfig workerPoolCfg = ThreadPoolConfig.defaultConfig();
        workerPoolCfg.setCorePoolSize(workerPoolSize).setMaxPoolSize(workerPoolSize);

        final TCPNIOTransport tcpTransport = TCPNIOTransportBuilder.newInstance().setReuseAddress(true).setIOStrategy(WorkerThreadIOStrategy.getInstance())
                .setSelectorThreadPoolConfig(selectorPoolCfg).setWorkerThreadPoolConfig(workerPoolCfg).build();
        try {
            tcpTransport.start();
        } finally {
            tcpTransport.shutdownNow();
        }
    }
}
