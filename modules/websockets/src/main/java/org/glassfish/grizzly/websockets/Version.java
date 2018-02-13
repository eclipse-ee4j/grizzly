/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.websockets.rfc6455.RFC6455Handler;

public enum Version {
    RFC6455("13") {
        @Override
        public ProtocolHandler createHandler(boolean mask) {
            return new RFC6455Handler(mask);
        }

        @Override
        public boolean validate(MimeHeaders headers) {
            return this.wireProtocolVersion.equals(headers.getHeader(Constants.SEC_WS_VERSION));
        }
    };

    public abstract ProtocolHandler createHandler(boolean mask);

    public abstract boolean validate(MimeHeaders headers);

    final String wireProtocolVersion;

    Version(final String wireProtocolVersion) {
        this.wireProtocolVersion = wireProtocolVersion;
    }

    @Override
    public String toString() {
        return name();
    }

    public static String getSupportedWireProtocolVersions() {
        final StringBuilder sb = new StringBuilder();
        for (Version v : Version.values()) {
            if (v.wireProtocolVersion.length() > 0) {
                sb.append(v.wireProtocolVersion).append(", ");
            }
        }
        return sb.substring(0, sb.length() - 2);

    }
}
