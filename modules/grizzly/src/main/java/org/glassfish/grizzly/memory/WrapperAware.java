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

package org.glassfish.grizzly.memory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.glassfish.grizzly.Buffer;

/**
 * {@link MemoryManager}s, which implement this interface, are able to convert frequently used Java buffer types to
 * Grizzly {@link Buffer}.
 *
 * @see MemoryUtils
 * @see MemoryManager
 *
 * @author Alexey Stashok
 */
public interface WrapperAware {
    /**
     * Returns {@link Buffer}, which wraps the byte array.
     *
     * @param data byte array to wrap
     *
     * @return {@link Buffer} wrapper on top of passed byte array.
     */
    Buffer wrap(byte[] data);

    /**
     * Returns {@link Buffer}, which wraps the part of byte array with specific offset and length.
     *
     * @param data byte array to wrap
     * @param offset byte buffer offset
     * @param length byte buffer length
     *
     * @return {@link Buffer} wrapper on top of passed byte array.
     */
    Buffer wrap(byte[] data, int offset, int length);

    /**
     * Returns {@link Buffer}, which wraps the {@link String}.
     *
     * @param s {@link String}
     *
     * @return {@link Buffer} wrapper on top of passed {@link String}.
     */
    Buffer wrap(String s);

    /**
     * Returns {@link Buffer}, which wraps the {@link String} with the specific {@link Charset}.
     *
     * @param s {@link String}
     * @param charset {@link Charset}, which will be used, when converting {@link String} to byte array.
     *
     * @return {@link Buffer} wrapper on top of passed {@link String}.
     */
    Buffer wrap(String s, Charset charset);

    /**
     * Returns {@link Buffer}, which wraps the {@link ByteBuffer}.
     *
     * @param byteBuffer {@link ByteBuffer} to wrap
     *
     * @return {@link Buffer} wrapper on top of passed {@link ByteBuffer}.
     */
    Buffer wrap(ByteBuffer byteBuffer);
}
