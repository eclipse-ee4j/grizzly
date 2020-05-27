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

import java.io.IOException;
import java.nio.channels.SelectionKey;

import org.glassfish.grizzly.IOEvent;

/**
 * {@link SelectionKeyHandler} implementations are responsible for handling {@link SelectionKey} life cycle events.
 *
 * @author Alexey Stashok
 */
public interface SelectionKeyHandler {

    /**
     * <p>
     * The default {@link SelectionKeyHandler} used by all created builder instances.
     * </p>
     *
     * <p>
     * The default may be changed by setting the system property
     * <code>org.glassfish.grizzly.DEFAULT_SELECTION_KEY_HANDLER</code> with the fully qualified name of the class that
     * implements the SelectionKeyHandler interface. Note that this class must be public and have a public no-arg
     * constructor.
     * </p>
     */
    SelectionKeyHandler DEFAULT_SELECTION_KEY_HANDLER = SelectionKeyHandlerInitializer.initHandler();

    void onKeyRegistered(SelectionKey key);

    void onKeyDeregistered(SelectionKey key);

    boolean onProcessInterest(SelectionKey key, int interest) throws IOException;

    void cancel(SelectionKey key) throws IOException;

    NIOConnection getConnectionForKey(SelectionKey selectionKey);

    void setConnectionForKey(NIOConnection connection, SelectionKey selectionKey);

    int ioEvent2SelectionKeyInterest(IOEvent ioEvent);

    IOEvent selectionKeyInterest2IoEvent(int selectionKeyInterest);

    IOEvent[] getIOEvents(int interest);
}
