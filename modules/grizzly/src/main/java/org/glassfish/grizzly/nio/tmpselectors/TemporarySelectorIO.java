/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio.tmpselectors;

import org.glassfish.grizzly.Grizzly;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.logging.Level;
import org.glassfish.grizzly.Reader;
import org.glassfish.grizzly.Writer;
import java.util.logging.Logger;
import org.glassfish.grizzly.localization.LogMessages;

/**
 *
 * @author oleksiys
 */
public class TemporarySelectorIO {
    private static final Logger LOGGER = Grizzly.logger(TemporarySelectorIO.class);

    protected TemporarySelectorPool selectorPool;

    private final Reader<SocketAddress> reader;
    private final Writer<SocketAddress> writer;

    public TemporarySelectorIO(Reader<SocketAddress> reader,
            Writer<SocketAddress> writer) {
        this(reader, writer, null);
    }

    public TemporarySelectorIO(Reader<SocketAddress> reader,
            Writer<SocketAddress> writer,
            TemporarySelectorPool selectorPool) {
        this.reader = reader;
        this.writer = writer;
        this.selectorPool = selectorPool;
    }

    public TemporarySelectorPool getSelectorPool() {
        return selectorPool;
    }

    public void setSelectorPool(TemporarySelectorPool selectorPool) {
        this.selectorPool = selectorPool;
    }

    public Reader<SocketAddress> getReader() {
        return reader;
    }

    public Writer<SocketAddress> getWriter() {
        return writer;
    }

    protected void recycleTemporaryArtifacts(Selector selector,
            SelectionKey selectionKey) {
        
        if (selectionKey != null) {
            try {
                selectionKey.cancel();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        LogMessages.WARNING_GRIZZLY_TEMPORARY_SELECTOR_IO_CANCEL_KEY_EXCEPTION(selectionKey),
                        e);
            }
        }

        if (selector != null) {
            selectorPool.offer(selector);
        }
    }
}
