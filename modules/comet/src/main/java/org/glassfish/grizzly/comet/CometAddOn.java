/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.comet;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.HttpServerProbe;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;

/**
 * Comet {@link AddOn} for the {@link HttpServer}.
 * 
 * @author Alexey Stashok
 */
public class CometAddOn implements AddOn {

    @Override
    public void setup(final NetworkListener networkListener,
            final FilterChainBuilder builder) {
        
        
        final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);
        final HttpServerFilter httpServerFilter =
                (HttpServerFilter) builder.get(httpServerFilterIdx);
        httpServerFilter.getMonitoringConfig().addProbes(new HttpServerProbe.Adapter() {
            @Override
            public void onBeforeServiceEvent(final HttpServerFilter filter,
                    final Connection connection, final Request request,
                    final HttpHandler httpHandler) {
                CometContext.REQUEST_LOCAL.set(request);
            }
        });

        CometEngine.getEngine().setCometSupported(true);
    }    
}
