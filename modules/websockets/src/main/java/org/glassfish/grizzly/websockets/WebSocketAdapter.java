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

package org.glassfish.grizzly.websockets;

public class WebSocketAdapter implements WebSocketListener {
    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
    }

    @Override
    public void onConnect(WebSocket socket) {
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
    }

    @Override
    public void onMessage(WebSocket socket, byte[] bytes) {
    }

    @Override
    public void onPing(WebSocket socket, byte[] bytes) {
    }

    @Override
    public void onPong(WebSocket socket, byte[] bytes) {
    }

    @Override
    public void onFragment(WebSocket socket, String fragment, boolean last) {
    }

    @Override
    public void onFragment(WebSocket socket, byte[] fragment, boolean last) {
    }
}
