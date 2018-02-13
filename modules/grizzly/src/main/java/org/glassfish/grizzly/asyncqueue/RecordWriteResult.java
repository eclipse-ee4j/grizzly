/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.asyncqueue;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.utils.Holder;

/**
 * Write result associated with a {@link AsyncWriteQueueRecord}.
 *
 * @param <K>
 * @param <L>
 * 
 * @author Alexey Stashok
 */
public final class RecordWriteResult<K, L> extends WriteResult<K, L> {
    private long lastWrittenBytes;
    private long bytesToReleaseAfterLastWrite;
    
    /**
     *  Settable destination address
     */
    private final SettableHolder<L> dstAddressHolder = new SettableHolder<L>();
    
    @Override
    protected void set(final Connection<L> connection, final K message,
            final L dstAddress, final long writtenSize) {
        super.set(connection, message, dstAddress, writtenSize);
    }

    @Override
    protected Holder<L> createAddrHolder(final L dstAddress) {
        return dstAddressHolder.set(dstAddress);
    }

    public long lastWrittenBytes() {
        return lastWrittenBytes;
    }

    public long bytesToReleaseAfterLastWrite() {
        return bytesToReleaseAfterLastWrite;
    }

    public RecordWriteResult<K, L> lastWriteResult(
            final long lastWrittenBytes,
            final long bytesToReleaseAfterLastWrite) {
        this.lastWrittenBytes = lastWrittenBytes;
        this.bytesToReleaseAfterLastWrite = bytesToReleaseAfterLastWrite;
        
        return this;
    }
    
    @Override
    public void recycle() {
        lastWrittenBytes = 0;
        bytesToReleaseAfterLastWrite = 0;
        dstAddressHolder.obj = null;
        
        reset();
    }
    
    private static class SettableHolder<L> extends Holder<L> {
        private L obj;
                
        public SettableHolder<L> set(final L obj) {
            this.obj = obj;
            return this;
        }
        @Override
        public L get() {
            return obj;
        }
    }
}
