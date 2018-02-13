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

package org.glassfish.grizzly.http.ajp;

import java.nio.ByteBuffer;

public class AjpDataPacket extends AjpPacket {
    private final byte[] data;

    public AjpDataPacket(byte[] data) {
        this.data = data;
    }

    @Override
    protected ByteBuffer buildContent() {
        final ByteBuffer buffer = ByteBuffer.allocate(data.length + 2);
        buffer.put((byte) 0);
        buffer.put((byte) data.length);
        buffer.put(data);
        buffer.flip();

        return buffer;
    }
}
