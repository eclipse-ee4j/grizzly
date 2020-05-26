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

package org.glassfish.grizzly.nio.transport;

import java.io.IOException;
import java.net.SocketAddress;

import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.tmpselectors.TemporarySelectorWriter;

/**
 *
 * @author oleksiys
 */
public final class UDPNIOTemporarySelectorWriter extends TemporarySelectorWriter {
    public UDPNIOTemporarySelectorWriter(UDPNIOTransport transport) {
        super(transport);
    }

    @Override
    protected long writeNow0(NIOConnection connection, SocketAddress dstAddress, WritableMessage message,
            WriteResult<WritableMessage, SocketAddress> currentResult) throws IOException {

        return ((UDPNIOTransport) transport).write((UDPNIOConnection) connection, dstAddress, message, currentResult);
    }
}
