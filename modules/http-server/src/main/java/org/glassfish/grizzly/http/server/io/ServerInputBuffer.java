/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.io;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpBrokenContent;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.io.InputBuffer;
import org.glassfish.grizzly.http.server.Request;

/**
 * Server-side implementation of the {@link InputBuffer}.
 *
 * @author Alexey Stashok
 */
public class ServerInputBuffer extends InputBuffer {
    private volatile long totalReadContentInBytes;
    private volatile Request serverRequest;

    public void initialize(final Request serverRequest, final FilterChainContext ctx) {
        this.serverRequest = serverRequest;
        super.initialize(serverRequest.getRequest(), ctx);
    }

    /**
     * Initiates asynchronous data receiving.
     *
     * This is service method, usually users don't have to call it explicitly.
     */
    @Override
    public void initiateAsyncronousDataReceiving() {
        if (!checkChunkedMaxPostSize()) {
            final HttpContent brokenContent = HttpBrokenContent.builder(serverRequest.getRequest())
                    .error(new IOException("The HTTP request content exceeds max post size")).build();
            try {
                append(brokenContent);
            } catch (IOException ignored) {
            }

            return;
        }

        super.initiateAsyncronousDataReceiving();
    }

    @Override
    protected HttpContent blockingRead() throws IOException {
        if (!checkChunkedMaxPostSize()) {
            throw new IOException("The HTTP request content exceeds max post size");
        }

        return super.blockingRead();
    }

    @Override
    protected void updateInputContentBuffer(final Buffer buffer) {
        totalReadContentInBytes += buffer.remaining();
        super.updateInputContentBuffer(buffer);
    }

    @Override
    public void recycle() {
        serverRequest = null;
        totalReadContentInBytes = 0;
        super.recycle();
    }

    @Override
    protected Executor getThreadPool() {
        return serverRequest.getRequestExecutor();
    }

    private boolean checkChunkedMaxPostSize() {
        if (serverRequest.getRequest().isChunked()) {
            final long maxPostSize = serverRequest.getHttpFilter().getConfiguration().getMaxPostSize();
            return maxPostSize < 0 || maxPostSize > totalReadContentInBytes;
        }

        return true;
    }
}
