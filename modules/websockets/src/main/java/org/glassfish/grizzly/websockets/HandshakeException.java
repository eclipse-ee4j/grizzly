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

package org.glassfish.grizzly.websockets;

import org.glassfish.grizzly.http.util.HttpStatus;

/**
 * {@link Exception}, which describes the error, occurred during the {@link WebSocket}
 * handshake phase.
 * 
 * @author Alexey Stashok
 */
public class HandshakeException extends WebSocketException {
    private final int code;

    /**
     * Construct a <tt>HandshakeException</tt>.
     *
     * @param message error description
     */
    public HandshakeException(String message) {
        this(HttpStatus.BAD_REQUEST_400.getStatusCode(), message);
    }

    /**
     * Construct a <tt>HandshakeException</tt>.
     *
     * @param code error code
     * @param message error description
     */
    public HandshakeException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Get the error code.
     *
     * @return the error code.
     */
    public int getCode() {
        return code;
    }
}
