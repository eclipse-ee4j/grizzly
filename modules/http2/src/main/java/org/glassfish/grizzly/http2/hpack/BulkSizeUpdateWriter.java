/*
 * Copyright (c) 2016, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2.hpack;

import org.glassfish.grizzly.Buffer;

import java.util.Iterator;

import static java.util.Objects.requireNonNull;

final class BulkSizeUpdateWriter implements BinaryRepresentationWriter {

    private final SizeUpdateWriter writer = new SizeUpdateWriter();
    private Iterator<Integer> maxSizes;
    private boolean writing;
    private boolean configured;

    BulkSizeUpdateWriter maxHeaderTableSizes(Iterable<Integer> sizes) {
        if (configured) {
            throw new IllegalStateException("Already configured");
        }
        requireNonNull(sizes, "sizes");
        maxSizes = sizes.iterator();
        configured = true;
        return this;
    }

    @Override
    public boolean write(HeaderTable table, Buffer destination) {
        if (!configured) {
            throw new IllegalStateException("Configure first");
        }
        while (true) {
            if (writing) {
                if (!writer.write(table, destination)) {
                    return false;
                }
                writing = false;
            } else if (maxSizes.hasNext()) {
                writing = true;
                writer.reset();
                writer.maxHeaderTableSize(maxSizes.next());
            } else {
                configured = false;
                return true;
            }
        }
    }

    @Override
    public BulkSizeUpdateWriter reset() {
        maxSizes = null;
        writing = false;
        configured = false;
        return this;
    }
}
