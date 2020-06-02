/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.io;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.InputSource;

/**
 * Adds the ability for binary based {@link org.glassfish.grizzly.InputSource}s to obtain the incoming
 * {@link org.glassfish.grizzly.Buffer} directly without having to use intermediate objects to copy the data to.
 *
 * @since 2.0
 */
public interface BinaryNIOInputSource extends InputSource {

    /**
     * <p>
     * Returns the the duplicate of the underlying {@link org.glassfish.grizzly.Buffer} that backs this
     * <code>InputSource</code>. The content of the returned buffer will be that of the underlying buffer. Changes to
     * returned buffer's content will be visible in the underlying buffer, and vice versa; the two buffers' position, limit,
     * and mark values will be independent.
     * </p>
     *
     * @return the duplicate of the underlying {@link org.glassfish.grizzly.Buffer} that backs this
     * <code>InputSource</code>.
     */
    Buffer getBuffer();

    /**
     * <p>
     * Returns the underlying {@link org.glassfish.grizzly.Buffer} that backs this <code>InputSource</code>. Unlike
     * {@link #getBuffer()}, this method detaches the returned {@link Buffer}, so user becomes responsible for handling the
     * {@link Buffer}'s life-cycle.
     * </p>
     *
     * @return the underlying {@link org.glassfish.grizzly.Buffer} that backs this <code>InputSource</code>.
     */
    Buffer readBuffer();

    /**
     * <p>
     * Returns the {@link org.glassfish.grizzly.Buffer} of a given size, which represents a chunk of the underlying
     * {@link org.glassfish.grizzly.Buffer} that backs this <code>InputSource</code>. Unlike {@link #getBuffer()}, this
     * method detaches the returned {@link Buffer}, so user becomes responsible for handling the {@link Buffer}'s
     * life-cycle.
     * </p>
     *
     * @param size the requested size of the {@link Buffer} to be returned.
     *
     * @return the {@link Buffer} of a given size, which represents a chunk of the underlying {@link Buffer} which contains
     * incoming request data. This method detaches the returned {@link Buffer}, so user code becomes responsible for
     * handling its life-cycle.
     */
    Buffer readBuffer(int size);

}
