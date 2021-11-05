/*
 * Copyright (c) 2011, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Payara Services Ltd.
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

package org.glassfish.grizzly;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import org.glassfish.grizzly.asyncqueue.WritableMessage;

public interface FileChunk extends WritableMessage {

    /**
     * Transfers the File region backing this <code>FileRegion</code> to the specified {@link WritableByteChannel}.
     *
     * @param c the {@link WritableByteChannel}
     * @return the number of bytes that have been transferred
     * @throws IOException if an error occurs while processing
     * @see java.nio.channels.FileChannel#transferTo(long, long, java.nio.channels.WritableByteChannel)
     */
    long writeTo(final WritableByteChannel c) throws IOException;

}
