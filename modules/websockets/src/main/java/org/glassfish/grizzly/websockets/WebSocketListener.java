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

/**
 * Interface to allow notification of events occurring on specific {@link WebSocket} instances.
 */
public interface WebSocketListener {

    /**
     * <p>
     * Invoked when {@link WebSocket#onClose(DataFrame)} has been called on a particular {@link WebSocket} instance.
     * <p>
     * 
     * @param socket the {@link WebSocket} being closed.
     * @param frame the closing {@link DataFrame} sent by the remote end-point.
     */
    void onClose(WebSocket socket, DataFrame frame);

    /**
     * <p>
     * Invoked when the opening handshake has been completed for a specific {@link WebSocket} instance.
     * </p>
     *
     * @param socket the newly connected {@link WebSocket}
     */
    void onConnect(WebSocket socket);

    /**
     * <p>
     * Invoked when {@link WebSocket#onMessage(String)} has been called on a particular {@link WebSocket} instance.
     * </p>
     *
     * @param socket the {@link WebSocket} that received a message.
     * @param text the message received.
     */
    void onMessage(WebSocket socket, String text);

    /**
     * <p>
     * Invoked when {@link WebSocket#onMessage(String)} has been called on a particular {@link WebSocket} instance.
     * </p>
     *
     * @param socket the {@link WebSocket} that received a message.
     * @param bytes the message received.
     */
    void onMessage(WebSocket socket, byte[] bytes);

    /**
     * <p>
     * Invoked when {@link WebSocket#onPing(DataFrame)} has been called on a particular {@link WebSocket} instance.
     * </p>
     *
     * @param socket the {@link WebSocket} that received the ping.
     * @param bytes the payload of the ping frame, if any.
     */
    void onPing(WebSocket socket, byte[] bytes);

    /**
     * <p>
     * Invoked when {@link WebSocket#onPong(DataFrame)} has been called on a particular {@link WebSocket} instance.
     * </p>
     *
     * @param socket the {@link WebSocket} that received the pong.
     * @param bytes the payload of the pong frame, if any.
     */
    void onPong(WebSocket socket, byte[] bytes);

    /**
     * <p>
     * Invoked when {@link WebSocket#onFragment(boolean, String)} has been called on a particular {@link WebSocket}
     * instance.
     * </p>
     *
     * @param socket the {@link WebSocket} received the message fragment.
     * @param fragment the message fragment.
     * @param last flag indicating if this was the last fragment.
     */
    void onFragment(WebSocket socket, String fragment, boolean last);

    /**
     * <p>
     * Invoked when {@link WebSocket#onFragment(boolean, byte[])} has been called on a particular {@link WebSocket}
     * instance.
     * </p>
     *
     * @param socket the {@link WebSocket} received the message fragment.
     * @param fragment the message fragment.
     * @param last flag indicating if this was the last fragment.
     */
    void onFragment(WebSocket socket, byte[] fragment, boolean last);

}
