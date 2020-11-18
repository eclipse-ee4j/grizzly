/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.portunif.finders;

import static org.glassfish.grizzly.ssl.SSLUtils.getSSLPacketSize;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.portunif.PUContext;
import org.glassfish.grizzly.portunif.ProtocolFinder;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;

/**
 *
 * @author Alexey Stashok
 */
public class SSLProtocolFinder implements ProtocolFinder {

    private static final Logger LOGGER = Grizzly.logger(SSLProtocolFinder.class);

    private final SSLEngineConfigurator sslEngineConfigurator;

    public SSLProtocolFinder(final SSLEngineConfigurator sslEngineConfigurator) {
        this.sslEngineConfigurator = sslEngineConfigurator;
    }

    @Override
    public Result find(final PUContext puContext, final FilterChainContext ctx) {
        final Buffer buffer = ctx.getMessage();
        try {
            final int expectedLength = getSSLPacketSize(buffer);
            if (expectedLength == -1 || buffer.remaining() < expectedLength) {
                return Result.NEED_MORE_DATA;
            }
        } catch (SSLException e) {
            LOGGER.log(Level.FINE, "Packet header is not SSL", e);
            return Result.NOT_FOUND;
        }

        return Result.FOUND;
    }
}
