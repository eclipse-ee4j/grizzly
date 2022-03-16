/*
 * Copyright (c) 2022 Eclipse Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.servlet;

import jakarta.servlet.ServletConnection;

import org.glassfish.grizzly.http.Protocol;


/**
 * Trivial implementation of the {@link ServletConnection}
 *
 * @author David Matejcek
 */
public class ServletConnectionImpl implements ServletConnection {

    private final String connectionId;
    private final Protocol protocol;
    private final boolean secure;

    /**
     * Just sets all fields.
     *
     * @param connectionId - see {@link #getConnectionId()}, can be null.
     * @param protocol - see {@link #getProtocol()} and {@link Protocol}, can be null.
     * @param secure - true if the connection was encrypted.
     */
    public ServletConnectionImpl(String connectionId, Protocol protocol, boolean secure) {
        this.connectionId = connectionId;
        this.protocol = protocol;
        this.secure = secure;
    }

    @Override
    public String getConnectionId() {
        return this.connectionId;
    }


    @Override
    public String getProtocol() {
        return protocol == null ? "unknown" : this.protocol.getProtocolString();
    }


    @Override
    public String getProtocolConnectionId() {
        // we don't support HTTP3 yet.
        return "";
    }


    @Override
    public boolean isSecure() {
        return this.secure;
    }
}
