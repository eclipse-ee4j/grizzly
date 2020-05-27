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

package org.glassfish.grizzly.http2;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.npn.AlpnClientNegotiator;

class AlpnClientNegotiatorImpl extends AlpnNegotiatorBase implements AlpnClientNegotiator {
    private final static Logger LOGGER = Grizzly.logger(AlpnClientNegotiatorImpl.class);

    private final Http2ClientFilter filter;

    public AlpnClientNegotiatorImpl(final Http2ClientFilter filter) {
        this.filter = filter;
    }

    @Override
    public String[] getProtocols(final SSLEngine sslEngine) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Alpn getProtocols. Connection={0}, protocols={1}",
                    new Object[] { AlpnSupport.getConnection(sslEngine), Arrays.toString(SUPPORTED_PROTOCOLS) });
        }
        return SUPPORTED_PROTOCOLS.clone();
    }

    @Override
    public void protocolSelected(final SSLEngine sslEngine, final String selectedProtocol) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Alpn protocolSelected. Connection={0}, protocol={1}",
                    new Object[] { AlpnSupport.getConnection(sslEngine), selectedProtocol });
        }

        final Connection connection = AlpnSupport.getConnection(sslEngine);
        if (HTTP2.equals(selectedProtocol)) {
            final Http2Session http2Session = filter.createClientHttp2Session(connection);

            // we expect preface
            http2Session.getHttp2State().setDirectUpgradePhase();

            http2Session.sendPreface();
        } else {
            // Never try HTTP2 for this connection
            Http2State.create(connection).setNeverHttp2();
        }
    }

}
