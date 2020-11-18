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
import java.util.LinkedHashMap;
import java.util.Map;

import org.glassfish.grizzly.http.util.MimeHeaders;

public class AjpForwardRequestPacket extends AjpPacket {
    private final String method;
    private final String resource;
    private final MimeHeaders headers = new MimeHeaders();
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final int port;

    public AjpForwardRequestPacket(String method, String resource, int port, int remotePort) {
        this.method = method;
        this.resource = resource;
        this.port = port;
        attributes.put("AJP_REMOTE_PORT", remotePort + "");
    }

    public void addHeader(String header, String value) {
        headers.addValue(header).setString(value);
    }

    @Override
    protected ByteBuffer buildContent() {
        ByteBuffer header = ByteBuffer.allocate(2);
        header.put(AjpConstants.JK_AJP13_FORWARD_REQUEST);
        header.put(Utils.getMethodCode(method));
        header = putString(header, "HTTP/1.1");
        header = putString(header, resource);
        header = putString(header, "127.0.0.1");
        header = putString(header, null);
        header = putString(header, "localhost");
        header = putShort(header, (short) port);
        header = ensureCapacity(header, 1).put((byte) 0);
        header = putHeaders(header);
        header = putAttributes(header);

        header.flip();
        return header;
    }

    private ByteBuffer putAttributes(ByteBuffer header) {
        ByteBuffer buffer = header;
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            buffer = ensureCapacity(buffer, 1).put(AjpConstants.SC_A_REQ_ATTRIBUTE);
            buffer = putString(buffer, entry.getKey());
            buffer = putString(buffer, entry.getValue());

        }
        buffer = ensureCapacity(buffer, 1).put(AjpConstants.SC_A_ARE_DONE);

        return buffer;
    }

    private ByteBuffer putHeaders(ByteBuffer header) {
        ByteBuffer buffer = header;
        if (headers.getValue("host") == null) {
            headers.addValue("host").setString("localhost:" + port);
        }
        buffer = putShort(buffer, (short) headers.size());
        final Iterable<String> names = headers.names();
        for (String name : names) {
            final byte headerCode = Utils.getHeaderCode(name);
            if (headerCode != -1) {
                buffer = putShort(buffer, (short) (0xA000 | headerCode));
            } else {
                buffer = putString(buffer, name);
            }
            buffer = putString(buffer, headers.getHeader(name));
        }

        return buffer;
    }
}
