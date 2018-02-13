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

final class LiteralWithIndexingWriter extends IndexNameValueWriter {

    private boolean tableUpdated;

    private CharSequence name;
    private CharSequence value;
    private int index;

    LiteralWithIndexingWriter() {
        super(0b0100_0000, 6);
    }

    @Override
    LiteralWithIndexingWriter index(int index) {
        super.index(index);
        this.index = index;
        return this;
    }

    @Override
    LiteralWithIndexingWriter name(CharSequence name, boolean useHuffman) {
        super.name(name, useHuffman);
        this.name = name;
        return this;
    }

    @Override
    LiteralWithIndexingWriter value(CharSequence value, boolean useHuffman) {
        super.value(value, useHuffman);
        this.value = value;
        return this;
    }

    @Override
    public boolean write(HeaderTable table, Buffer destination) {
        if (!tableUpdated) {
            CharSequence n;
            if (indexedRepresentation) {
                n = table.get(index).name;
            } else {
                n = name;
            }
            table.put(n, value);
            tableUpdated = true;
        }
        return super.write(table, destination);
    }

    @Override
    public IndexNameValueWriter reset() {
        tableUpdated = false;
        name = null;
        value = null;
        index = -1;
        return super.reset();
    }
}
