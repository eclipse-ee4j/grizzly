/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

/**
 * General Broadcaster API to send the same message to a set of clients.
 *
 * @author Alexey Stashok
 */
public interface Broadcaster {

    /**
     * Broadcasts the provided <tt>text</tt> content to the specified recipients.
     *
     * @param recipients the recipients of the provided <tt>text</tt> content.
     * @param text textual content.
     */
    void broadcast(final Iterable<? extends WebSocket> recipients, final String text);

    /**
     * Broadcasts the provided <tt>binary</tt> content to the specified recipients.
     *
     * @param recipients the recipients of the provided <tt>binary</tt> content.
     * @param binary binary content.
     */
    void broadcast(final Iterable<? extends WebSocket> recipients, final byte[] binary);

    /**
     * Broadcasts the provided fragmented <tt>text</tt> content to the specified recipients.
     *
     * @param recipients the recipients of the provided fragmented <tt>text</tt> content.
     * @param text fragmented textual content.
     * @param last <tt>true</tt> if this is the last fragment, otherwise <tt>false</tt>.
     */
    void broadcastFragment(final Iterable<? extends WebSocket> recipients, final String text, final boolean last);

    /**
     * Broadcasts the provided fragmented <tt>binary</tt> content to the specified recipients.
     *
     * @param recipients the recipients of the provided fragmented <tt>binary</tt> content.
     * @param binary fragmented binary content.
     * @param last <tt>true</tt> if this is the last fragment, otherwise <tt>false</tt>.
     */
    void broadcastFragment(final Iterable<? extends WebSocket> recipients, final byte[] binary, final boolean last);
}
