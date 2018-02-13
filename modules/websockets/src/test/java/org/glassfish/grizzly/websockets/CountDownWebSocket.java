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

import java.io.IOException;
import java.net.URISyntaxException;

import org.glassfish.grizzly.GrizzlyFuture;

public class CountDownWebSocket extends WebSocketClient {
    private final Object sync = new Object();
    private int countDown;

    public CountDownWebSocket(String url, Version version, WebSocketListener... listeners) throws IOException, URISyntaxException {
        super(url, version, listeners);
    }

    @Override
    public GrizzlyFuture<DataFrame> send(String data) {
        synchronized(sync) {
            countDown++;
        }
        
        return super.send(data);
    }

    @Override
    public void onMessage(String data) {
        synchronized(sync) {
            if (--countDown == 0) {
                sync.notify();
            }
        }
    }

    public boolean countDown() {
        synchronized(sync) {
            if (countDown == 0) return true;
            
            try {
                sync.wait(30000);
            } catch (InterruptedException e) {
            }
            
            return countDown == 0;
        }
    }
}
