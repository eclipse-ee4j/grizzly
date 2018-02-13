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
import org.glassfish.grizzly.ReadResult;

/**
 * Read result associated with a {@link AsyncWriteQueueRecord}.
 *
 * @param <K>
 * @param <L>
 * 
 * @author Alexey Stashok
 */
public final class RecordReadResult<K, L> extends ReadResult<K, L> {

    @Override
    protected void set(final Connection<L> connection, final K message,
            final L srcAddress, final int readSize) {
        super.set(connection, message, srcAddress, readSize);
    }
    
    @Override
    public void recycle() {
        reset();
    }
}
