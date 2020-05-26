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

package org.glassfish.grizzly.http.io;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.OutputSink;

/**
 * Adds the ability for binary based {@link org.glassfish.grizzly.OutputSink}s to write a {@link Buffer} instead of
 * having to convert to those types supported by {@link java.io.OutputStream}.
 *
 * @since 2.0
 */
public interface BinaryNIOOutputSink extends OutputSink {

    /**
     * Writes the contents of the specified {@link org.glassfish.grizzly.Buffer}.
     *
     * @param buffer the {@link org.glassfish.grizzly.Buffer to write}
     */
    void write(final Buffer buffer) throws IOException;

}
