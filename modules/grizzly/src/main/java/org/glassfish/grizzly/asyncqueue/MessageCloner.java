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

package org.glassfish.grizzly.asyncqueue;

import org.glassfish.grizzly.Connection;

/**
 * Cloner, which will be called by {@link AsyncQueueWriter}, when message could not be written directly, and will be
 * added to the queue. Cloner may create a clone of original message and return it to the {@link AsyncQueueWriter}
 * instead of original one. Using MessageCloner, developer has a chance to clone a message only in case, when it is
 * really required.
 *
 * @author Alexey Stashok
 */
public interface MessageCloner<E> {
    /**
     * Method will be called by {@link AsyncQueueWriter}, when message could not be written directly, and will be added to
     * the queue. Cloner may create a clone of original message and return it to the {@link AsyncQueueWriter} instead of
     * original one. Using MessageCloner, developer has a chance to clone a message only in case, when it is really
     * required.
     *
     * @param connection {@link Connection}, where the {@link org.glassfish.grizzly.Buffer} will be written.
     * @param originalMessage {@link org.glassfish.grizzly.Buffer} to be written.
     *
     * @return original {@link org.glassfish.grizzly.Buffer} or its clone to be added to asynchronous queue.
     */
    E clone(Connection connection, E originalMessage);
}
