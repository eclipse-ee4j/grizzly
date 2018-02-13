/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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
 * The default {@link Broadcaster} optimized to send the same text to a set of
 * clients.
 * NOTE: works with {@link DefaultWebSocket}s and inherited classes.
 * 
 * @author Alexey Stashok
 */
public class OptimizedBroadcaster implements Broadcaster {

    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(final Iterable<? extends WebSocket> recipients,
            final String text) {
        
        byte[] rawDataToSend = null;
        
        for (WebSocket websocket : recipients) {
            final DefaultWebSocket defaultWebSocket = (DefaultWebSocket) websocket;
            
            if (websocket.isConnected()) {
                if (rawDataToSend == null) {
                    rawDataToSend = defaultWebSocket.toRawData(text);
                }
                
                try {
                    defaultWebSocket.sendRaw(rawDataToSend);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void broadcast(final Iterable<? extends WebSocket> recipients,
            final byte[] binary) {
        
        byte[] rawDataToSend = null;
        
        for (WebSocket websocket : recipients) {
            final DefaultWebSocket defaultWebSocket = (DefaultWebSocket) websocket;
            
            if (websocket.isConnected()) {
                if (rawDataToSend == null) {
                    rawDataToSend = defaultWebSocket.toRawData(binary);
                }
                
                try {
                    defaultWebSocket.sendRaw(rawDataToSend);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }

    @Override
    public void broadcastFragment(Iterable<? extends WebSocket> recipients,
            String text, boolean last) {
        byte[] rawDataToSend = null;
        
        for (WebSocket websocket : recipients) {
            final DefaultWebSocket defaultWebSocket = (DefaultWebSocket) websocket;
            
            if (websocket.isConnected()) {
                if (rawDataToSend == null) {
                    rawDataToSend = defaultWebSocket.toRawData(text, last);
                }
                
                try {
                    defaultWebSocket.sendRaw(rawDataToSend);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }

    @Override
    public void broadcastFragment(Iterable<? extends WebSocket> recipients, byte[] binary, boolean last) {
        byte[] rawDataToSend = null;
        
        for (WebSocket websocket : recipients) {
            final DefaultWebSocket defaultWebSocket = (DefaultWebSocket) websocket;
            
            if (websocket.isConnected()) {
                if (rawDataToSend == null) {
                    rawDataToSend = defaultWebSocket.toRawData(binary, last);
                }
                
                try {
                    defaultWebSocket.sendRaw(rawDataToSend);
                } catch (WebSocketException ignored) {
                }
            }
        }
    }
}
