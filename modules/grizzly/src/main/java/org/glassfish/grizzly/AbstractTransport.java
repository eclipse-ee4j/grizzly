/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.monitoring.DefaultMonitoringConfig;
import org.glassfish.grizzly.monitoring.MonitoringAware;
import org.glassfish.grizzly.monitoring.MonitoringConfig;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.grizzly.threadpool.ThreadPoolProbe;
import org.glassfish.grizzly.utils.StateHolder;

/**
 * Abstract {@link Transport}. Implements common transport functionality.
 *
 * @author Alexey Stashok
 */
public abstract class AbstractTransport implements Transport {
    /**
     * Transport name
     */
    protected String name;

    /**
     * Transport mode
     */
    protected volatile boolean isBlocking;

    @Deprecated
    protected volatile boolean isStandalone;

    /**
     * Transport state controller
     */
    protected final StateHolder<State> state;

    /**
     * Transport default Processor
     */
    protected Processor processor;

    /**
     * Transport default ProcessorSelector
     */
    protected ProcessorSelector processorSelector;

    /**
     * Transport strategy
     */
    protected IOStrategy strategy;

    /**
     * Transport MemoryManager
     */
    protected MemoryManager memoryManager;

    /**
     * Worker thread pool
     */
    protected ExecutorService workerThreadPool;

    /**
     * Kernel thread pool.
     */
    protected ExecutorService kernelPool;

    /**
     * Transport AttributeBuilder, which will be used to create Attributes
     */
    protected AttributeBuilder attributeBuilder;

    /**
     * Transport default buffer size for read operations
     */
    protected int readBufferSize;

    /**
     * Transport default buffer size for write operations
     */
    protected int writeBufferSize;

    protected ThreadPoolConfig workerPoolConfig;

    protected ThreadPoolConfig kernelPoolConfig;

    protected boolean managedWorkerPool = true;

    protected long writeTimeout = TimeUnit.MILLISECONDS.convert(DEFAULT_WRITE_TIMEOUT, TimeUnit.SECONDS);

    protected long readTimeout = TimeUnit.MILLISECONDS.convert(DEFAULT_READ_TIMEOUT, TimeUnit.SECONDS);

    /**
     * Transport probes
     */
    protected final DefaultMonitoringConfig<TransportProbe> transportMonitoringConfig = new DefaultMonitoringConfig<>(TransportProbe.class) {

        @Override
        public Object createManagementObject() {
            return createJmxManagementObject();
        }
    };

    /**
     * Connection probes
     */
    protected final DefaultMonitoringConfig<ConnectionProbe> connectionMonitoringConfig = new DefaultMonitoringConfig<>(ConnectionProbe.class);

    /**
     * Thread pool probes
     */
    protected final DefaultMonitoringConfig<ThreadPoolProbe> threadPoolMonitoringConfig = new DefaultMonitoringConfig<>(ThreadPoolProbe.class);

    public AbstractTransport(String name) {
        this.name = name;
        state = new StateHolder<>(State.STOPPED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setName(String name) {
        this.name = name;
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBlocking() {
        return isBlocking;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configureBlocking(boolean isBlocking) {
        this.isBlocking = isBlocking;
        notifyProbesConfigChanged(this);
    }

    @Override
    public boolean isStandalone() {
        return isStandalone;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StateHolder<State> getState() {
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReadBufferSize() {
        return readBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStopped() {
        final State currentState = state.getState();
        return currentState == State.STOPPED || currentState == State.STOPPING;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPaused() {
        return state.getState() == State.PAUSED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor obtainProcessor(IOEvent ioEvent, Connection connection) {
        if (processor != null && processor.isInterested(ioEvent)) {
            return processor;
        } else if (processorSelector != null) {
            return processorSelector.select(ioEvent, connection);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor getProcessor() {
        return processor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcessor(Processor processor) {
        this.processor = processor;
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessorSelector getProcessorSelector() {
        return processorSelector;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setProcessorSelector(ProcessorSelector selector) {
        processorSelector = selector;
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IOStrategy getIOStrategy() {
        return strategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIOStrategy(IOStrategy IOStrategy) {
        this.strategy = IOStrategy;
        final ThreadPoolConfig strategyConfig = IOStrategy.createDefaultWorkerPoolConfig(this);
        if (strategyConfig == null) {
            workerPoolConfig = null;
        } else {
            if (workerPoolConfig == null) {
                setWorkerThreadPoolConfig(strategyConfig);
            }
        }
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        notifyProbesConfigChanged(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutorService getWorkerThreadPool() {
        return workerThreadPool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutorService getKernelThreadPool() {
        return kernelPool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKernelThreadPool(ExecutorService kernelPool) {
        this.kernelPool = kernelPool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setKernelThreadPoolConfig(ThreadPoolConfig kernelPoolConfig) {
        if (isStopped()) {
            this.kernelPoolConfig = kernelPoolConfig;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWorkerThreadPoolConfig(ThreadPoolConfig workerPoolConfig) {
        if (isStopped()) {
            this.workerPoolConfig = workerPoolConfig;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadPoolConfig getKernelThreadPoolConfig() {
        return isStopped() ? kernelPoolConfig : kernelPoolConfig.copy();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadPoolConfig getWorkerThreadPoolConfig() {
        return isStopped() ? workerPoolConfig : workerPoolConfig.copy();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setWorkerThreadPool(final ExecutorService threadPool) {
        managedWorkerPool = false;
        if (threadPool instanceof MonitoringAware) {
            if (threadPoolMonitoringConfig.hasProbes()) {
                ((MonitoringAware<ThreadPoolProbe>) threadPool).getMonitoringConfig().addProbes(threadPoolMonitoringConfig.getProbes());
            }
        }

        setWorkerThreadPool0(threadPool);
    }

    protected void setWorkerThreadPool0(final ExecutorService threadPool) {
        this.workerThreadPool = threadPool;
        notifyProbesConfigChanged(this);
    }

    protected void setKernelPool0(final ExecutorService kernelPool) {
        this.kernelPool = kernelPool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeBuilder getAttributeBuilder() {
        return attributeBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttributeBuilder(AttributeBuilder attributeBuilder) {
        this.attributeBuilder = attributeBuilder;
        notifyProbesConfigChanged(this);
    }

    /**
     * Close the connection, managed by Transport
     *
     * @param connection {@link org.glassfish.grizzly.nio.NIOConnection} to close
     * @throws IOException not used
     */
    protected abstract void closeConnection(Connection connection) throws IOException;

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitoringConfig<ConnectionProbe> getConnectionMonitoringConfig() {
        return connectionMonitoringConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitoringConfig<TransportProbe> getMonitoringConfig() {
        return transportMonitoringConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MonitoringConfig<ThreadPoolProbe> getThreadPoolMonitoringConfig() {
        return threadPoolMonitoringConfig;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getReadTimeout(TimeUnit timeUnit) {
        if (readTimeout <= 0) {
            return -1;
        } else {
            return timeUnit.convert(readTimeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadTimeout(long timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            readTimeout = -1;
        } else {
            readTimeout = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getWriteTimeout(TimeUnit timeUnit) {
        if (writeTimeout <= 0) {
            return -1;
        } else {
            return timeUnit.convert(writeTimeout, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteTimeout(long timeout, TimeUnit timeUnit) {
        if (timeout <= 0) {
            writeTimeout = -1;
        } else {
            writeTimeout = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
        }
    }

    /**
     * {@inheritDoc} Notify registered {@link TransportProbe}s about the before-start event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     *
     * @since 3.0
     */
    protected static void notifyProbesBeforeStart(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onBeforeStartEvent(transport);
            }
        }
    }

    /**
     * Notify registered {@link TransportProbe}s about the before-stop event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     *
     * @since 3.0
     */
    protected static void notifyProbesBeforeStop(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onBeforeStopEvent(transport);
            }
        }
    }

    /**
     * Notify registered {@link TransportProbe}s about the stop event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     */
    protected static void notifyProbesStop(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onStopEvent(transport);
            }
        }
    }

    /**
     * {@inheritDoc} Notify registered {@link TransportProbe}s about the before-pause event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     *
     * @since 3.0
     */
    protected static void notifyProbesBeforePause(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onBeforePauseEvent(transport);
            }
        }
    }

    /**
     * Notify registered {@link TransportProbe}s about the pause event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     */
    protected static void notifyProbesPause(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onPauseEvent(transport);
            }
        }
    }

    /**
     * Notify registered {@link TransportProbe}s about the before-resume event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     *
     * @since 3.0
     */
    protected static void notifyProbesBeforeResume(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onBeforeStartEvent(transport);
            }
        }
    }

    /**
     * Notify registered {@link TransportProbe}s about the config changed event.
     *
     * @param transport the <tt>Transport</tt> event occurred on.
     */
    protected static void notifyProbesConfigChanged(final AbstractTransport transport) {
        final TransportProbe[] probes = transport.transportMonitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (TransportProbe probe : probes) {
                probe.onConfigChangeEvent(transport);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public void stop() throws IOException {
        shutdownNow();
    }

    /**
     * Create the Transport JMX management object.
     *
     * @return the Transport JMX management object.
     */
    protected abstract Object createJmxManagementObject();
}
