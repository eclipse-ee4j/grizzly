/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.npn.AlpnClientNegotiator;
import org.glassfish.grizzly.npn.AlpnServerNegotiator;
import org.glassfish.grizzly.npn.NegotiationSupport;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.glassfish.grizzly.ssl.SSLUtils;

/**
 * Grizzly TLS Next Protocol Negotiation support class.
 * 
 */
public class AlpnSupport {
    private final static Logger LOGGER = Grizzly.logger(AlpnSupport.class);

    private final static Map<SSLEngine, Connection<?>> SSL_TO_CONNECTION_MAP =
            new WeakHashMap<>();
    
    private static final AlpnSupport INSTANCE;
    
    static {
        
        boolean isExtensionFound = false;
        try {
            ClassLoader.getSystemClassLoader().loadClass("sun.security.ssl.GrizzlyNPN");
            isExtensionFound = true;
        } catch (Throwable e) {
            LOGGER.log(Level.FINE, "TLS ALPN extension is not found:", e);
        }
        
        INSTANCE = isExtensionFound ? new AlpnSupport() : null;
    }

    public static boolean isEnabled() {
        return INSTANCE != null;
    }

    public static AlpnSupport getInstance() {
        if (!isEnabled()) {
            throw new IllegalStateException("TLS ALPN is disabled");
        }
        
        return INSTANCE;
    }

    public static Connection<?> getConnection(final SSLEngine engine) {
        synchronized (SSL_TO_CONNECTION_MAP) {
            return SSL_TO_CONNECTION_MAP.get(engine);
        }
    }
    
    private static void setConnection(final SSLEngine engine,
            final Connection<?> connection) {
        synchronized (SSL_TO_CONNECTION_MAP) {
            SSL_TO_CONNECTION_MAP.put(engine, connection);
        }
    }

    private final Map<Object, AlpnServerNegotiator> serverSideNegotiators =
            new WeakHashMap<>();
    private final ReadWriteLock serverSideLock = new ReentrantReadWriteLock();
    
    private final Map<Object, AlpnClientNegotiator> clientSideNegotiators =
            new WeakHashMap<>();
    private final ReadWriteLock clientSideLock = new ReentrantReadWriteLock();

    private final SSLFilter.HandshakeListener handshakeListener = 
            new SSLFilter.HandshakeListener() {

        @Override
        public void onStart(final Connection<?> connection) {
            final SSLEngine sslEngine = SSLUtils.getSSLEngine(connection);
            assert sslEngine != null;
            
            if (sslEngine.getUseClientMode()) {
                AlpnClientNegotiator negotiator;
                clientSideLock.readLock().lock();
                
                try {
                    negotiator = clientSideNegotiators.get(connection);
                    if (negotiator == null) {
                        negotiator = clientSideNegotiators.get(connection.getTransport());
                    }
                } finally {
                    clientSideLock.readLock().unlock();
                }
                
                if (negotiator != null) {
                    // add a CloseListener to ensure we remove the
                    // negotiator associated with this SSLEngine
                    connection.addCloseListener(new CloseListener<Closeable, CloseType>() {
                        @Override
                        public void onClosed(Closeable closeable, CloseType type) throws IOException {
                            NegotiationSupport.removeAlpnClientNegotiator(sslEngine);
                            SSL_TO_CONNECTION_MAP.remove(sslEngine);
                        }
                    });
                    setConnection(sslEngine, connection);
                    NegotiationSupport.addNegotiator(sslEngine, negotiator);
                }
            } else {
                AlpnServerNegotiator negotiator;
                serverSideLock.readLock().lock();
                
                try {
                    negotiator = serverSideNegotiators.get(connection);
                    if (negotiator == null) {
                        negotiator = serverSideNegotiators.get(connection.getTransport());
                    }
                } finally {
                    serverSideLock.readLock().unlock();
                }
                
                if (negotiator != null) {

                    // add a CloseListener to ensure we remove the
                    // negotiator associated with this SSLEngine
                    connection.addCloseListener(new CloseListener<Closeable, CloseType>() {
                        @Override
                        public void onClosed(Closeable closeable, CloseType type) throws IOException {
                            NegotiationSupport.removeAlpnServerNegotiator(sslEngine);
                            SSL_TO_CONNECTION_MAP.remove(sslEngine);
                        }
                    });
                    setConnection(sslEngine, connection);
                    NegotiationSupport.addNegotiator(sslEngine, negotiator);
                }
            }
            
        }

        @Override
        public void onComplete(final Connection<?> connection) {
        }

        @Override
        public void onFailure(Connection<?> connection, Throwable t) {
        }
    };
    
    private AlpnSupport() {
    }    
    
    public void configure(final SSLBaseFilter sslFilter) {
        sslFilter.addHandshakeListener(handshakeListener);
    }
    
    public void setServerSideNegotiator(final Transport transport,
            final AlpnServerNegotiator negotiator) {
        putServerSideNegotiator(transport, negotiator);
    }
    
    public void setServerSideNegotiator(final Connection<?> connection,
            final AlpnServerNegotiator negotiator) {
        putServerSideNegotiator(connection, negotiator);
    }

    
    public void setClientSideNegotiator(final Transport transport,
            final AlpnClientNegotiator negotiator) {
        putClientSideNegotiator(transport, negotiator);
    }

    public void setClientSideNegotiator(final Connection<?> connection,
            final AlpnClientNegotiator negotiator) {
        putClientSideNegotiator(connection, negotiator);
    }

    private void putServerSideNegotiator(final Object object,
            final AlpnServerNegotiator negotiator) {
        serverSideLock.writeLock().lock();

        try {
            serverSideNegotiators.put(object, negotiator);
        } finally {
            serverSideLock.writeLock().unlock();
        }
    }

    private void putClientSideNegotiator(final Object object,
            final AlpnClientNegotiator negotiator) {
        clientSideLock.writeLock().lock();

        try {
            clientSideNegotiators.put(object, negotiator);
        } finally {
            clientSideLock.writeLock().unlock();
        }
    }

}
