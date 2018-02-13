/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2.frames;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.memory.MemoryManager;

import java.util.Collections;
import java.util.Map;

/**
 * Holder class when we encounter frames of an unknown type.
 */
public class UnknownFrame extends Http2Frame {

    private final int type;
    private final int length;

    public UnknownFrame(final int type, final int length) {
        this.type = type;
        this.length = length;
    }

    @Override
    public Buffer toBuffer(final MemoryManager memoryManager) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int calcLength() {
        return length;
    }

    @Override
    protected Map<Integer, String> getFlagNamesMap() {
        return Collections.emptyMap();
    }

    @Override
    public int getType() {
        return type;
    }
}
