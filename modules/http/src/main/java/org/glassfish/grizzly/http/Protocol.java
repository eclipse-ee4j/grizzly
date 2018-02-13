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

import java.io.UnsupportedEncodingException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.util.BufferChunk;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.utils.Charsets;

/**
 * Predefined HTTP protocol versions
 * 
 * @author Alexey Stashok
 */
public enum Protocol {
    HTTP_0_9 (0, 9),
    HTTP_1_0 (1, 0),
    HTTP_1_1 (1, 1),
    HTTP_2_0 (2, 0);

    public static Protocol valueOf(final byte[] protocolBytes,
            final int offset, final int len) {
        if (len == 0) {
            return Protocol.HTTP_0_9;
        } else if (equals(HTTP_1_1, protocolBytes, offset, len)) {
            return Protocol.HTTP_1_1;
        } else if (equals(HTTP_1_0, protocolBytes, offset, len)) {
            return Protocol.HTTP_1_0;
        } else if (equals(HTTP_2_0, protocolBytes, offset, len)) {
            return Protocol.HTTP_2_0;
        } else if (equals(HTTP_0_9, protocolBytes, offset, len)) {
            return Protocol.HTTP_0_9;
        }
        
        throw new IllegalStateException("Unknown protocol " +
                new String(protocolBytes, offset, len, Charsets.ASCII_CHARSET));
    }

    public static Protocol valueOf(final Buffer protocolBuffer,
                                   final int offset, final int len) {
        if (len == 0) {
            return Protocol.HTTP_0_9;
        } else if (equals(HTTP_1_1, protocolBuffer, offset, len)) {
            return Protocol.HTTP_1_1;
        } else if (equals(HTTP_1_0, protocolBuffer, offset, len)) {
            return Protocol.HTTP_1_0;
        } else if (equals(HTTP_2_0, protocolBuffer, offset, len)) {
            return Protocol.HTTP_2_0;
        } else if (equals(HTTP_0_9, protocolBuffer, offset, len)) {
            return Protocol.HTTP_0_9;
        }

        throw new IllegalStateException("Unknown protocol " +
                protocolBuffer.toStringContent(Charsets.ASCII_CHARSET, offset, len));
    }

    public static Protocol valueOf(final DataChunk protocolC) {
        if (protocolC.getLength() == 0) {
            return Protocol.HTTP_0_9;
        } else if (protocolC.equals(Protocol.HTTP_1_1.getProtocolBytes())) {
            return Protocol.HTTP_1_1;
        } else if (protocolC.equals(Protocol.HTTP_1_0.getProtocolBytes())) {
            return Protocol.HTTP_1_0;
        } else if (protocolC.equals(Protocol.HTTP_2_0.getProtocolBytes())) {
            return Protocol.HTTP_2_0;
        } else if (protocolC.equals(Protocol.HTTP_0_9.getProtocolBytes())) {
            return Protocol.HTTP_0_9;
        }
        
        throw new IllegalStateException("Unknown protocol " + protocolC.toString());
    }    

    private static boolean equals(final Protocol protocol,
            final byte[] protocolBytes,
            final int offset, final int len) {
        
        final byte[] knownProtocolBytes = protocol.getProtocolBytes();
        
        return ByteChunk.equals(knownProtocolBytes, 0, knownProtocolBytes.length,
                protocolBytes, offset, len);
    }

    private static boolean equals(final Protocol protocol,
                                  final Buffer protocolBuffer,
                                  final int offset,
                                  final int len) {


        final byte[] knownProtocolBytes = protocol.getProtocolBytes();
        return BufferChunk.equals(knownProtocolBytes, 0, knownProtocolBytes.length,
                protocolBuffer, offset, len);
    }
    
    private final String protocolString;
    private final int majorVersion;
    private final int minorVersion;
    private final byte[] protocolBytes;

    Protocol(final int majorVersion, final int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.protocolString = "HTTP/" + majorVersion + '.' + minorVersion;
        
        byte[] protocolBytes0;
        try {
            protocolBytes0 = protocolString.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ignored) {
            protocolBytes0 = protocolString.getBytes(Charsets.ASCII_CHARSET);
        }
        
        this.protocolBytes = protocolBytes0;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public String getProtocolString() {
        return protocolString;
    }

    public byte[] getProtocolBytes() {
        return protocolBytes;
    }

    public boolean equals(final String s) {
        return s != null &&
                ByteChunk.equals(protocolBytes, 0, protocolBytes.length, s);
    }
    
    public boolean equals(final DataChunk protocolC) {
        return protocolC != null &&
                !protocolC.isNull() &&
                protocolC.equals(protocolBytes);
    }
}
