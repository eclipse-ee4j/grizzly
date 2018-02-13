/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetSocketAddress;
import org.glassfish.grizzly.Connection;

/**
 * The client-side SNI config resolver, which could be used to customize
 * SNI host name sent from client to server.
 * 
 * By default the host name is based on {@link Connection#getPeerAddress()} value,
 * particularly for TCP connections the default value will be based on
 * {@link InetSocketAddress#getHostString()}. But <code>SNIClientConfigResolver</code>
 * allows you to customize the default host name value.
 * 
 * @author Alexey Stashok
 */
public interface SNIClientConfigResolver {

    /**
     * Returns {@link SNIConfig} for the new {@link Connection}, <code>null</code>
     * value means no SNI information will be sent.
     * 
     * The {@link SNIConfig} could be created like:
     * 
     * <pre>
     * {@code
     *      SNIConfig.clientConfigBuilder()
     *               .host("myhost.com")
     *               .sslEngineConfigurator(myHostSSLEngineConfigurator)
     *               .build();
     * }
     * </pre>
     * 
     * @param connection
     * @return {@link SNIConfig} for the new {@link Connection}, <code>null</code>
     * value means no SNI information will be sent
     */
    SNIConfig resolve(Connection connection);
}
