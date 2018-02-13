/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.sni.httpserver;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.sni.SNIConfig;
import org.glassfish.grizzly.sni.SNIServerConfigResolver;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * The {@link SNIServerConfigResolver}, that manages SSL configuration for
 * "*.foo.com" and "*.bar.com" virtual hosts. Connections to other virtual hosts
 * will be terminated.
 */
public class FooBarSNIResolver implements SNIServerConfigResolver {

    private final SNIConfig fooConfig;
    private final SNIConfig barConfig;
    
    public FooBarSNIResolver(final SSLEngineConfigurator fooSSLEngineConfig,
            final SSLEngineConfigurator barSSLEngineConfig) {
        fooConfig = SNIConfig.newServerConfig(fooSSLEngineConfig);
        barConfig = SNIConfig.newServerConfig(barSSLEngineConfig);
    }

    @Override
    public SNIConfig resolve(Connection connection, String hostname) {
        if (hostname.equals("foo.com") || hostname.endsWith(".foo.com")) {
            return fooConfig;
        } else if (hostname.equals("bar.com") || hostname.endsWith(".bar.com")) {
            return barConfig;
        }

        // unknown host - terminate the connection
        return SNIConfig.failServerConfig(hostname);
    }
}
