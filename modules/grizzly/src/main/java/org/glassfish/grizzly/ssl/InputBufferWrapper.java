/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.ssl;

import org.glassfish.grizzly.memory.ByteBufferWrapper;
import org.glassfish.grizzly.nio.DirectByteBufferRecord;

/**
 * Input {@link org.glassfish.grizzly.Buffer} to read SSL packets to.
 * This {@link org.glassfish.grizzly.Buffer} has to be used by a Transport
 * to read data.
 * 
 * @author Alexey Stashok
 */
final class InputBufferWrapper extends ByteBufferWrapper {
    private DirectByteBufferRecord record;

    public InputBufferWrapper() {
    }

    public InputBufferWrapper prepare(final int size) {
        final DirectByteBufferRecord recordLocal = DirectByteBufferRecord.get();
        this.record = recordLocal;
        this.visible = recordLocal.allocate(size);
        
        return this;
    }

    @Override
    public void dispose() {
        record.release();
        record = null;
        super.dispose();
    }
    
}
