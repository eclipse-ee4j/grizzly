/*
 * Copyright (c) 2014, 2021 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.sni;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.utils.Futures;
import org.glassfish.grizzly.utils.StringFilter;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Basic SNI test
 * 
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class SNITest {
    public static final int PORT = 19283;

    @Test
    public void testClientServerSNI() throws Exception {
        final String sniHostValue = "sni-test.com";
        final String msg = "Hello world!";
        
        final Attribute<String> sniHostAttr =
                Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("sni-host-attr");

        final SSLEngineConfigurator sslServerEngineConfig = 
                    new SSLEngineConfigurator(
                            createSSLContextConfigurator().createSSLContext(),
                    false, false, false);
        final SSLEngineConfigurator sslClientEngineConfig = 
                    new SSLEngineConfigurator(
                            createSSLContextConfigurator().createSSLContext(),
                    true, false, false);
        
        final SNIFilter sniFilter = new SNIFilter();
        sniFilter.setServerSSLConfigResolver(new SNIServerConfigResolver() {

            @Override
            public SNIConfig resolve(Connection connection, String hostname) {
                sniHostAttr.set(connection, hostname);
                
                return SNIConfig.newServerConfig(sslServerEngineConfig);
            }
        });
        
        sniFilter.setClientSSLConfigResolver(new SNIClientConfigResolver() {

            @Override
            public SNIConfig resolve(Connection connection) {
                return SNIConfig.newClientConfig(sniHostValue,
                        sslClientEngineConfig);
            }
        });

        final FutureImpl<String[]> resultFuture = Futures.createSafeFuture();
        final FilterChain chain = FilterChainBuilder.stateless()
                .add(new TransportFilter())
                .add(sniFilter)
                .add(new StringFilter())
                .add(new BaseFilter() {

                    @Override
                    public NextAction handleRead(final FilterChainContext ctx)
                            throws IOException {
                        final String msg = ctx.getMessage();
                        final String sniHost = sniHostAttr.get(ctx.getConnection());
                        
                        resultFuture.result(new String[] {msg, sniHost});
                        return ctx.getInvokeAction();
                    }

                })
                .build();
        
        TCPNIOTransport transport = TCPNIOTransportBuilder.newInstance()
                .setProcessor(chain)
                .build();
        
        try {
            transport.bind(PORT);
            transport.start();
            
            final Connection c = transport.connect("localhost", PORT).get();
            c.write(msg);
            
            final String[] result = resultFuture.get(10, TimeUnit.SECONDS);
            assertEquals(msg, result[0]);
            assertEquals(sniHostValue, result[1]);
            
        } finally {
            transport.shutdownNow();
        }
    }
    
    private static SSLContextConfigurator createSSLContextConfigurator() {
        SSLContextConfigurator sslContextConfigurator =
                new SSLContextConfigurator();
        ClassLoader cl = SNITest.class.getClassLoader();
        // override system properties
        URL cacertsUrl = cl.getResource("ssltest-cacerts.jks");
        if (cacertsUrl != null) {
            sslContextConfigurator.setTrustStoreFile(cacertsUrl.getFile());
            sslContextConfigurator.setTrustStorePass("changeit");
        }

        // override system properties
        URL keystoreUrl = cl.getResource("ssltest-keystore.jks");
        if (keystoreUrl != null) {
            sslContextConfigurator.setKeyStoreFile(keystoreUrl.getFile());
            sslContextConfigurator.setKeyStorePass("changeit");
        }

        return sslContextConfigurator;
    }    
}
