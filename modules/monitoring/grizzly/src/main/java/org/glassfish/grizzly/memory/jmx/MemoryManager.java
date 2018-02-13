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

package org.glassfish.grizzly.memory.jmx;

import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.grizzly.memory.MemoryProbe;
import java.util.concurrent.atomic.AtomicLong;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.gmbal.NameValue;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * {@link org.glassfish.grizzly.memory.MemoryManager} JMX object.
 *
 * @author Alexey Stashok
 */
@ManagedObject
@Description("Grizzly Memory Manager")
public class MemoryManager extends JmxObject {

    protected final org.glassfish.grizzly.memory.MemoryManager memoryManager;
    private final MemoryProbe probe;

    private final AtomicLong totalAllocatedBytes = new AtomicLong();
    private final AtomicLong realAllocatedBytes = new AtomicLong();
    private final AtomicLong poolAllocatedBytes = new AtomicLong();
    private final AtomicLong poolReleasedBytes = new AtomicLong();
    
    public MemoryManager(org.glassfish.grizzly.memory.MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        probe = new JmxMemoryProbe();
    }

    @Override
    public String getJmxName() {
        return "MemoryManager";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
        memoryManager.getMonitoringConfig().addProbes(probe);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onDeregister(GrizzlyJmxManager mom) {
        memoryManager.getMonitoringConfig().removeProbes(probe);
    }

    @NameValue
    public String getMemoryManagerType() {
        return memoryManager.getClass().getName();
    }

    @ManagedAttribute(id="total-allocated-bytes")
    @Description("Total number of allocated bytes (real + pool)")
    public long getTotalAllocatedBytes() {
        return totalAllocatedBytes.get();
    }

    @ManagedAttribute(id="real-allocated-bytes")
    @Description("Total number of bytes allocated using ByteBuffer.allocate(...) operation")
    public long getRealAllocatedBytes() {
        return realAllocatedBytes.get();
    }

    @ManagedAttribute(id="pool-allocated-bytes")
    @Description("Total number of bytes allocated from memory pool")
    public long getPoolAllocatedBytes() {
        return poolAllocatedBytes.get();
    }

    @ManagedAttribute(id="pool-released-bytes")
    @Description("Total number of bytes released to memory pool")
    public long getPoolReleasedBytes() {
        return poolReleasedBytes.get();
    }

    private class JmxMemoryProbe implements MemoryProbe {

        @Override
        public void onBufferAllocateEvent(int size) {
            totalAllocatedBytes.addAndGet(size);
            realAllocatedBytes.addAndGet(size);
        }

        @Override
        public void onBufferAllocateFromPoolEvent(int size) {
            totalAllocatedBytes.addAndGet(size);
            poolAllocatedBytes.addAndGet(size);
        }

        @Override
        public void onBufferReleaseToPoolEvent(int size) {
            poolReleasedBytes.addAndGet(size);
        }

    }
}
