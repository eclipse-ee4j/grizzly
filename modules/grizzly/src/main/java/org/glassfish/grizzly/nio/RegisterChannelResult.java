/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 *
 * @author Alexey Stashok
 */
public final class RegisterChannelResult {
    private final SelectorRunner selectorRunner;
    private final SelectionKey selectionKey;
    private final SelectableChannel channel;

    public RegisterChannelResult(SelectorRunner selectorRunner, SelectionKey selectionKey, SelectableChannel channel) {
        this.selectorRunner = selectorRunner;
        this.selectionKey = selectionKey;
        this.channel = channel;
    }

    public SelectorRunner getSelectorRunner() {
        return selectorRunner;
    }

    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public SelectableChannel getChannel() {
        return channel;
    }
}
