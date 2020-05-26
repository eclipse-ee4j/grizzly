/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.HttpContent;

/**
 * The {@link StreamInputBuffer} implementation, which is used when upgrading HTTP -> HTTP/2 connections.
 *
 * @author Alexey Stashok
 */
class UpgradeInputBuffer implements StreamInputBuffer {

    private boolean isClosed;
    private final Http2Stream stream;

    UpgradeInputBuffer(final Http2Stream stream) {
        this.stream = stream;
    }

    @Override
    public void onReadEventComplete() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean offer(Buffer data, boolean isLast) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public HttpContent poll() throws IOException {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void close(Termination termination) {
        terminate(termination);
    }

    @Override
    public void terminate(Termination termination) {
        synchronized (this) {
            if (isClosed) {
                return;
            }

            isClosed = true;
            stream.onInputClosed();
        }

        termination.doTask();
    }

    @Override
    public synchronized boolean isClosed() {
        return isClosed;
    }

}
