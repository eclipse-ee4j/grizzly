/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.streams;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;

/**
 *
 * @author oleksiys
 */
public class StreamOutput implements Output {
    private final StreamWriter streamWriter;

    public StreamOutput(StreamWriter streamWriter) {
        this.streamWriter = streamWriter;
    }

    @Override
    public void write(byte data) throws IOException {
        streamWriter.writeByte(data);
    }

    @Override
    public void write(Buffer buffer) throws IOException {
        streamWriter.writeBuffer(buffer);
    }

    @Override
    public boolean isBuffered() {
        return false;
    }

    @Override
    public void ensureBufferCapacity(int size) throws IOException {
    }

    @Override
    public Buffer getBuffer() {
        throw new UnsupportedOperationException("Buffer is not available in StreamOutput");
    }

    @Override
    public GrizzlyFuture<Integer> flush(CompletionHandler<Integer> completionHandler) throws IOException {
        return streamWriter.flush(completionHandler);
    }

    @Override
    public GrizzlyFuture<Integer> close(CompletionHandler<Integer> completionHandler) throws IOException {
        return streamWriter.close(completionHandler);
    }

}
