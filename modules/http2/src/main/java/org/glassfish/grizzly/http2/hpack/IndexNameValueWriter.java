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

abstract class IndexNameValueWriter implements BinaryRepresentationWriter {

    private final int pattern;
    private final int prefix;
    private final IntegerWriter intWriter = new IntegerWriter();
    private final StringWriter nameWriter = new StringWriter();
    private final StringWriter valueWriter = new StringWriter();

    protected boolean indexedRepresentation;

    private static final int NEW               = 0;
    private static final int NAME_PART_WRITTEN = 1;
    private static final int VALUE_WRITTEN     = 2;

    private int state = NEW;

    protected IndexNameValueWriter(int pattern, int prefix) {
        this.pattern = pattern;
        this.prefix = prefix;
    }

    IndexNameValueWriter index(int index) {
        indexedRepresentation = true;
        intWriter.configure(index, prefix, pattern);
        return this;
    }

    IndexNameValueWriter name(CharSequence name, boolean useHuffman) {
        indexedRepresentation = false;
        intWriter.configure(0, prefix, pattern);
        nameWriter.configure(name, useHuffman);
        return this;
    }

    IndexNameValueWriter value(CharSequence value, boolean useHuffman) {
        valueWriter.configure(value, useHuffman);
        return this;
    }

    @Override
    public boolean write(HeaderTable table, Buffer destination) {
        if (state < NAME_PART_WRITTEN) {
            if (indexedRepresentation) {
                if (!intWriter.write(destination)) {
                    return false;
                }
            } else {
                if (!intWriter.write(destination) || !nameWriter.write(destination)) {
                    return false;
                }
            }
            state = NAME_PART_WRITTEN;
        }
        if (state < VALUE_WRITTEN) {
            if (!valueWriter.write(destination)) {
                return false;
            }
            state = VALUE_WRITTEN;
        }
        return state == VALUE_WRITTEN;
    }

    @Override
    public IndexNameValueWriter reset() {
        intWriter.reset();
        if (!indexedRepresentation) {
            nameWriter.reset();
        }
        valueWriter.reset();
        state = NEW;
        return this;
    }
}
