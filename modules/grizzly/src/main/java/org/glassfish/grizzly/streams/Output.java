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
public interface Output {
    void write(byte data) throws IOException;

    void write(Buffer buffer) throws IOException;

    boolean isBuffered();

    void ensureBufferCapacity(int size) throws IOException;

    /**
     * Return the <tt>Input</tt>'s {@link Buffer}.
     *
     * @return the <tt>Input</tt>'s {@link Buffer}.
     */
    Buffer getBuffer();

    /**
     * Make sure that all data that has been written is flushed from the stream to its destination.
     */
    GrizzlyFuture<Integer> flush(CompletionHandler<Integer> completionHandler) throws IOException;

    /**
     * Close the {@link StreamWriter} and make sure all data was flushed.
     */
    GrizzlyFuture<Integer> close(CompletionHandler<Integer> completionHandler) throws IOException;
}
