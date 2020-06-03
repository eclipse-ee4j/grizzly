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
import org.glassfish.grizzly.utils.conditions.Condition;

/**
 *
 * @author Alexey Stashok
 */
public class StreamInput implements Input {

    private final StreamReader streamReader;

    public StreamInput(StreamReader streamReader) {
        this.streamReader = streamReader;
    }

    @Override
    public GrizzlyFuture<Integer> notifyCondition(Condition condition, CompletionHandler<Integer> completionHandler) {
        return streamReader.notifyCondition(condition, completionHandler);
    }

    @Override
    public byte read() throws IOException {
        return streamReader.readByte();
    }

    @Override
    public void skip(int length) {
        streamReader.skip(length);
    }

    @Override
    public boolean isBuffered() {
        return streamReader.isSupportBufferWindow();
    }

    @Override
    public Buffer getBuffer() {
        return streamReader.getBufferWindow();
    }

    @Override
    public Buffer takeBuffer() {
        return streamReader.takeBufferWindow();
    }

    @Override
    public int size() {
        return streamReader.available();
    }

    @Override
    public void close() throws IOException {
        streamReader.close();
    }
}
