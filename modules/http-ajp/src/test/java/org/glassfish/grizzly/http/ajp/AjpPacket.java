/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

public abstract class AjpPacket {
    public static ByteBuffer putShort(ByteBuffer target, short value) {
        return ensureCapacity(target, 2).putShort(value);
    }

    public static ByteBuffer putString(ByteBuffer target, String value) {
        ByteBuffer buffer;
        if (value == null) {
            buffer = ensureCapacity(target, 2).putShort((short) 0xFFFF);
        } else {
            final byte[] bytes = value.getBytes();
            buffer = ensureCapacity(target, 3 + bytes.length).putShort((short) bytes.length);
            buffer.put(value.getBytes());
            buffer.put((byte) 0);
        }

        return buffer;
    }

    protected static ByteBuffer ensureCapacity(ByteBuffer buffer, int additional) {
        if (buffer.remaining() < additional) {
            final ByteBuffer expanded = ByteBuffer.allocate(buffer.capacity() + additional);
            buffer.flip();
            expanded.put(buffer);
            return expanded;
        }
        return buffer;
    }

    protected ByteBuffer buildPacketHeader(final short size) {
        ByteBuffer pktHeader = ByteBuffer.allocate(4);
        pktHeader.put((byte) 0x12);
        pktHeader.put((byte) 0x34);
        pktHeader.putShort(size);
        pktHeader.flip();
        return pktHeader;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer header = buildContent();
        ByteBuffer pktHeader = buildPacketHeader((short) header.remaining());

        ByteBuffer packet = ByteBuffer.allocate(pktHeader.remaining() + header.remaining());
        packet.put(pktHeader);
        packet.put(header);
        packet.flip();
        return packet;
    }

    public byte[] toByteArray() {
        final ByteBuffer byteBuffer = toBuffer();
        byte[] body = new byte[byteBuffer.remaining()];
        byteBuffer.get(body);

        return body;
    }

    @Override
    public String toString() {
        final ByteBuffer buffer = toBuffer();
        return new String(buffer.array(), buffer.position(), buffer.limit() - buffer.position());
    }

    protected abstract ByteBuffer buildContent();
}
