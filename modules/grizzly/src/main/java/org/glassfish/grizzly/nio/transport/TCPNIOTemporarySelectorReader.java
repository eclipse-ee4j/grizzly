/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.tmpselectors.TemporarySelectorReader;

/**
 *
 * @author oleksiys
 */
public final class TCPNIOTemporarySelectorReader extends TemporarySelectorReader {
    public TCPNIOTemporarySelectorReader(TCPNIOTransport transport) {
        super(transport);
    }

    @Override
    protected final int readNow0(final NIOConnection connection, Buffer buffer,
            final ReadResult<Buffer, SocketAddress> currentResult)
            throws IOException {
        final int oldPosition = buffer != null ? buffer.position() : 0;
        
        if ((buffer = ((TCPNIOTransport) transport).read(connection, buffer)) != null) {
            final int readBytes = buffer.position() - oldPosition;
            currentResult.setMessage(buffer);
            currentResult.setReadSize(currentResult.getReadSize() + readBytes);
            currentResult.setSrcAddressHolder(((TCPNIOConnection) connection).peerSocketAddressHolder);

            return readBytes;
        }

        return 0;
    }
}
