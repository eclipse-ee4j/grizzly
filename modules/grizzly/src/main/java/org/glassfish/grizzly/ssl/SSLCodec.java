/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.ssl;

import java.io.IOException;
import java.util.concurrent.Future;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Codec;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Transformer;

/**
 * SSL Codec, which contains SSL encoder and decoder {@link Transformer}s.
 * 
 * @author Alexey Stashok
 */
public class SSLCodec implements Codec<Buffer, Buffer> {

    private final SSLEngineConfigurator serverSSLEngineConfig;
    private final SSLEngineConfigurator clientSSLEngineConfig;
    
    private final Transformer<Buffer, Buffer> decoder;
    private final Transformer<Buffer, Buffer> encoder;

    public SSLCodec(SSLContextConfigurator config) {
        this(config.createSSLContext(true));
    }

    public SSLCodec(SSLContext sslContext) {

        decoder = new SSLDecoderTransformer();
        encoder = new SSLEncoderTransformer();

        serverSSLEngineConfig = new SSLEngineConfigurator(sslContext, false,
                false, false);
        clientSSLEngineConfig = new SSLEngineConfigurator(sslContext, true,
                false, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transformer<Buffer, Buffer> getDecoder() {
        return decoder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Transformer<Buffer, Buffer> getEncoder() {
        return encoder;
    }

    public SSLEngineConfigurator getClientSSLEngineConfig() {
        return clientSSLEngineConfig;
    }

    public SSLEngineConfigurator getServerSSLEngineConfig() {
        return serverSSLEngineConfig;
    }

    public Future<SSLEngine> handshake(Connection connection)
            throws IOException {
        return handshake(connection, clientSSLEngineConfig);
    }

    public Future<SSLEngine> handshake(Connection connection,
            SSLEngineConfigurator configurator) throws IOException {
        return null;
//        return sslHandshaker.handshake(connection, configurator);
    }
}
