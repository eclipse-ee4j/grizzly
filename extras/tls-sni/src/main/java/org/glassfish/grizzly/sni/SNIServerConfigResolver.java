/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 * The server-side SNI config resolver, which could be used to customize {@link SSLEngineConfigurator} based on the SNI
 * host name information sent from client.
 *
 * @author Alexey Stashok
 */
public interface SNIServerConfigResolver {

    /**
     * Returns {@link SNIConfig} for the new {@link Connection}, <code>null</code> value means use default
     * {@link SNIFilter#getServerSSLEngineConfigurator()}.
     *
     * The {@link SNIConfig} could be created like:
     *
     * <pre>
     * {@code
     *      SNIConfig.serverConfigBuilder()
     *               .sslEngineConfigurator(myHostSSLEngineConfigurator)
     *               .build();
     * }
     * </pre>
     *
     * @param connection
     * @param hostname the SNI host name sent by a client
     *
     * @return {@link SNIConfig} for the new {@link Connection}, <code>null</code> value means use default
     * {@link SNIFilter#getServerSSLEngineConfigurator()}
     */
    SNIConfig resolve(Connection connection, String hostname);

}
