/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Common interface for entities that may be written to a {@link java.nio.channels.Channel}.
 *
 * @since 2.2
 */
public interface WritableMessage {

    /**
     * Return <code>true</code> if this message has data remaining to be 
     * written.
     * 
     * @return <code>true</code> if this message has data remaining to 
     * be written.
     */
    boolean hasRemaining();


    /**
     * Return the number of bytes remaining to be written.
     * @return the number of bytes remaining to be written.
     */
    int remaining();


    /**
     * Perform message specific actions to release resources held by the
     * entity backing this <code>WritableMessage</code>.
     */
    boolean release();
    
    /**
     * Returns <tt>true</tt> if the message represents an external resource
     * (for example {@link org.glassfish.grizzly.FileTransfer}),
     * which is not loaded in memory.
     * 
     * <tt>False</tt>, if the message is
     * located in memory (like {@link org.glassfish.grizzly.Buffer}).
     */
    boolean isExternal();
}
