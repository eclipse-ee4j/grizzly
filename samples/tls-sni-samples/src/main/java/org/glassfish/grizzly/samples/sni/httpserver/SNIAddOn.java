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

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.sni.SNIFilter;
import org.glassfish.grizzly.sni.SNIServerConfigResolver;
import org.glassfish.grizzly.ssl.SSLBaseFilter;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * SNI {@link AddOn}, that replaces standard {@link SSLBaseFilter} with
 * an {@link SNIFilter} in order to use different {@link SSLEngineConfigurator}
 * depending on the SNI host information.
 */
public class SNIAddOn implements AddOn {
    private final SNIFilter sniFilter;
    private final SNIServerConfigResolver serverConfigResolver;
    
    public SNIAddOn(final SNIServerConfigResolver serverConfigResolver) {
        if (serverConfigResolver == null) {
            throw new IllegalArgumentException("serverConfigResolver can't be null");
        }
        
        this.serverConfigResolver = serverConfigResolver;
        sniFilter = null;
    }
    
    public SNIAddOn(final SNIFilter sniFilter) {
        if (sniFilter == null) {
            throw new IllegalArgumentException("sniFilter can't be null");
        }
        
        this.sniFilter = sniFilter;
        serverConfigResolver = null;
    }
    
    @Override
    public void setup(final NetworkListener networkListener,
            final FilterChainBuilder builder) {
        final int sslFilterIdx = builder.indexOfType(SSLBaseFilter.class);
        if (sslFilterIdx != -1) {
            // replace SSLBaseFilter with SNIFilter
            final SSLBaseFilter sslFilter =
                    (SSLBaseFilter) builder.get(sslFilterIdx);
            
            SNIFilter sniFilterLocal = sniFilter;
            if (sniFilterLocal == null) {
                sniFilterLocal = new SNIFilter(
                        sslFilter.getServerSSLEngineConfigurator(), // default SSLEngineConfigurator
                        null,
                        sslFilter.isRenegotiateOnClientAuthWant());
                sniFilterLocal.setServerSSLConfigResolver(serverConfigResolver);
            }
            
            builder.set(sslFilterIdx, sniFilterLocal);
        }
    }
}
