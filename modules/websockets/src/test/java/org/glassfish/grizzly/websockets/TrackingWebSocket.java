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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.GrizzlyFuture;

public class TrackingWebSocket extends WebSocketClient {
    final Set<String> sent = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    private final CountDownLatch received;
    private final String name;

    public TrackingWebSocket(String address, Version version, int count, WebSocketListener... listeners)
        throws IOException, URISyntaxException {
        this(address, null, version, count, listeners);
    }

    public TrackingWebSocket(String address, String name, Version version, int count, WebSocketListener... listeners)
        throws IOException, URISyntaxException {
        super(address, version, listeners);
        this.name = name;
        received = new CountDownLatch(count);
    }

    @Override
    public GrizzlyFuture<DataFrame> send(String data) {
        sent.add(data);
        return super.send(data);
    }

    @Override
    public void onMessage(String message) {
        super.onMessage(message);
        if (sent.remove(message)) {
            received.countDown();
        }
    }

    @Override
    public void onConnect() {
        super.onConnect();
    }

    public boolean waitOnMessages() throws InterruptedException {
        return received.await(WebSocketEngine.DEFAULT_TIMEOUT*10, TimeUnit.SECONDS);
    }

    public String getName() {
        return name;
    }

    public CountDownLatch getReceived() {
        return received;
    }
    
    @Override
    protected void buildTransport() {
        super.buildTransport();
        transport.getAsyncQueueIO().getWriter().setMaxPendingBytesPerConnection(-1);
    }
}
