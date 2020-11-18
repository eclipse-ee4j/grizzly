/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.asyncqueue.AsyncQueueWriter;
import org.glassfish.grizzly.asyncqueue.AsyncWriteQueueRecord;
import org.glassfish.grizzly.asyncqueue.RecordWriteResult;
import org.glassfish.grizzly.asyncqueue.WritableMessage;
import org.glassfish.grizzly.nio.AbstractNIOAsyncQueueWriter;
import org.glassfish.grizzly.nio.NIOConnection;
import org.glassfish.grizzly.nio.NIOTransport;

/**
 * The UDP transport {@link AsyncQueueWriter} implementation, based on the Java NIO
 *
 * @author Alexey Stashok
 */
public final class UDPNIOAsyncQueueWriter extends AbstractNIOAsyncQueueWriter {

    public UDPNIOAsyncQueueWriter(final NIOTransport transport) {
        super(transport);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RecordWriteResult write0(final NIOConnection connection, final AsyncWriteQueueRecord queueRecord) throws IOException {

        final RecordWriteResult<WritableMessage, SocketAddress> writeResult = queueRecord.getCurrentResult();

        if (queueRecord.remaining() == 0) {
            return writeResult.lastWriteResult(0, queueRecord.isUncountable() ? AsyncWriteQueueRecord.UNCOUNTABLE_RECORD_SPACE_VALUE : 0);

        }

        final WritableMessage outputMessage = queueRecord.getMessage();
        final SocketAddress dstAddress = (SocketAddress) queueRecord.getDstAddress();
        final long written = ((UDPNIOTransport) transport).write((UDPNIOConnection) connection, dstAddress, outputMessage, writeResult);

        return writeResult.lastWriteResult(written, written);
    }

    @Override
    protected void onReadyToWrite(final NIOConnection connection) throws IOException {
        connection.enableIOEvent(IOEvent.WRITE);
    }
}
