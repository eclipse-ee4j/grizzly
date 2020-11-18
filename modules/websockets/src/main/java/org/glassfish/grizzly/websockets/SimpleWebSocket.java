/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.EnumSet;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.websockets.frametypes.PingFrameType;
import org.glassfish.grizzly.websockets.frametypes.PongFrameType;

/**
 * Generic WebSocket implementation. Typically used by client implementations.
 */
public class SimpleWebSocket implements WebSocket {

    private static final Logger LOGGER = Grizzly.logger(DefaultWebSocket.class);

    protected final Queue<WebSocketListener> listeners = new ConcurrentLinkedQueue<>();

    protected final ProtocolHandler protocolHandler;

    protected Broadcaster broadcaster = new DummyBroadcaster();

    protected enum State {
        NEW, CONNECTED, CLOSING, CLOSED
    }

    protected final EnumSet<State> connected = EnumSet.range(State.CONNECTED, State.CLOSING);
    protected final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    public SimpleWebSocket(final ProtocolHandler protocolHandler, final WebSocketListener... listeners) {

        this.protocolHandler = protocolHandler;
        for (WebSocketListener listener : listeners) {
            add(listener);
        }

        protocolHandler.setWebSocket(this);
    }

    public Collection<WebSocketListener> getListeners() {
        return listeners;
    }

    @Override
    public final boolean add(WebSocketListener listener) {
        return listeners.add(listener);
    }

    @Override
    public final boolean remove(WebSocketListener listener) {
        return listeners.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return connected.contains(state.get());
    }

    public void setClosed() {
        state.set(State.CLOSED);
    }

    @Override
    public void onClose(final DataFrame frame) {
        if (state.compareAndSet(State.CONNECTED, State.CLOSING)) {
            final ClosingFrame closing = (ClosingFrame) frame;
            protocolHandler.close(closing.getCode(), closing.getTextPayload());
        } else {
            state.set(State.CLOSED);
            protocolHandler.doClose();
        }

        WebSocketListener listener;
        while ((listener = listeners.poll()) != null) {
            listener.onClose(this, frame);
        }
    }

    @Override
    public void onConnect() {
        state.set(State.CONNECTED);

        for (WebSocketListener listener : listeners) {
            listener.onConnect(this);
        }
    }

    @Override
    public void onFragment(boolean last, byte[] fragment) {
        for (WebSocketListener listener : listeners) {
            listener.onFragment(this, fragment, last);
        }
    }

    @Override
    public void onFragment(boolean last, String fragment) {
        for (WebSocketListener listener : listeners) {
            listener.onFragment(this, fragment, last);
        }
    }

    @Override
    public void onMessage(byte[] data) {
        for (WebSocketListener listener : listeners) {
            listener.onMessage(this, data);
        }
    }

    @Override
    public void onMessage(String text) {
        for (WebSocketListener listener : listeners) {
            listener.onMessage(this, text);
        }
    }

    @Override
    public void onPing(DataFrame frame) {
        send(new DataFrame(new PongFrameType(), frame.getBytes()));
        for (WebSocketListener listener : listeners) {
            listener.onPing(this, frame.getBytes());
        }
    }

    @Override
    public void onPong(DataFrame frame) {
        for (WebSocketListener listener : listeners) {
            listener.onPong(this, frame.getBytes());
        }
    }

    @Override
    public void close() {
        close(NORMAL_CLOSURE, null);
    }

    @Override
    public void close(int code) {
        close(code, null);
    }

    @Override
    public void close(int code, String reason) {
        if (state.compareAndSet(State.CONNECTED, State.CLOSING)) {
            protocolHandler.close(code, reason);
        }
    }

    @Override
    public GrizzlyFuture<DataFrame> send(byte[] data) {
        if (isConnected()) {
            return protocolHandler.send(data);
        } else {
            throw new RuntimeException("Socket is not connected.");
        }
    }

    @Override
    public GrizzlyFuture<DataFrame> send(String data) {
        if (isConnected()) {
            return protocolHandler.send(data);
        } else {
            throw new RuntimeException("Socket is not connected.");
        }
    }

    @Override
    public void broadcast(Iterable<? extends WebSocket> recipients, String data) {
        if (state.get() == State.CONNECTED) {
            broadcaster.broadcast(recipients, data);
        } else {
            throw new RuntimeException("Socket is already closed.");
        }
    }

    @Override
    public void broadcast(Iterable<? extends WebSocket> recipients, byte[] data) {
        if (state.get() == State.CONNECTED) {
            broadcaster.broadcast(recipients, data);
        } else {
            throw new RuntimeException("Socket is already closed.");
        }
    }

    @Override
    public void broadcastFragment(Iterable<? extends WebSocket> recipients, String data, boolean last) {
        if (state.get() == State.CONNECTED) {
            broadcaster.broadcastFragment(recipients, data, last);
        } else {
            throw new RuntimeException("Socket is already closed.");
        }
    }

    @Override
    public void broadcastFragment(Iterable<? extends WebSocket> recipients, byte[] data, boolean last) {
        if (state.get() == State.CONNECTED) {
            broadcaster.broadcastFragment(recipients, data, last);
        } else {
            throw new RuntimeException("Socket is already closed.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrizzlyFuture<DataFrame> sendPing(byte[] data) {
        return send(new DataFrame(new PingFrameType(), data));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GrizzlyFuture<DataFrame> sendPong(byte[] data) {
        return send(new DataFrame(new PongFrameType(), data));
    }

    private GrizzlyFuture<DataFrame> send(DataFrame frame) {
        if (isConnected()) {
            return protocolHandler.send(frame);
        } else {
            throw new RuntimeException("Socket is not connected.");
        }
    }

    @Override
    public GrizzlyFuture<DataFrame> stream(boolean last, String fragment) {
        if (isConnected()) {
            return protocolHandler.stream(last, fragment);
        } else {
            throw new RuntimeException("Socket is not connected.");
        }
    }

    @Override
    public GrizzlyFuture<DataFrame> stream(boolean last, byte[] bytes, int off, int len) {
        if (isConnected()) {
            return protocolHandler.stream(last, bytes, off, len);
        } else {
            throw new RuntimeException("Socket is not connected.");
        }
    }

    protected byte[] toRawData(String text) {
        return toRawData(text, true);
    }

    protected byte[] toRawData(byte[] binary) {
        return toRawData(binary, true);
    }

    protected byte[] toRawData(String fragment, boolean last) {
        final DataFrame dataFrame = protocolHandler.toDataFrame(fragment, last);
        return protocolHandler.frame(dataFrame);
    }

    protected byte[] toRawData(byte[] binary, boolean last) {
        final DataFrame dataFrame = protocolHandler.toDataFrame(binary, last);
        return protocolHandler.frame(dataFrame);
    }

    @SuppressWarnings("unchecked")
    protected void sendRaw(byte[] rawData) {
        final Connection connection = protocolHandler.getConnection();
        final MemoryManager mm = connection.getTransport().getMemoryManager();
        final Buffer buffer = Buffers.wrap(mm, rawData);
        buffer.allowBufferDispose(false);

        connection.write(buffer);
    }

    protected Broadcaster getBroadcaster() {
        return broadcaster;
    }

    protected void setBroadcaster(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }
}
