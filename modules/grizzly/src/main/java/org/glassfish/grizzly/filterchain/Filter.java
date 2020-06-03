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

package org.glassfish.grizzly.filterchain;

import java.io.IOException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.attributes.Attribute;

/**
 * A Filter encapsulates a unit of processing work to be performed, whose purpose is to examine and/or modify the state
 * of a transaction that is represented by a {@link FilterChainContext}. Individual Filter can be assembled into a
 * {@link FilterChain}, which allows them to either complete the required processing or delegate further processing to
 * the next Filter in the {@link FilterChain}.
 *
 * Filter implementations should be designed in a thread-safe manner, suitable for inclusion in multiple
 * {@link FilterChain} that might be processed by different threads simultaneously. In general, this implies that Filter
 * classes should not maintain state information in instance variables. Instead, state information should be maintained
 * via suitable modifications to the attributes of the {@link FilterChainContext} that is passed to the appropriate
 * {@link Filter} processing methods.
 *
 * Filter implementations typically retrieve and store state information in the {@link FilterChainContext} instance that
 * is passed as a parameter to the appropriate {@link Filter} processing methods, using particular {@link Attribute}s
 * that can be acquired via {@link Attribute#get(org.glassfish.grizzly.attributes.AttributeStorage)} method.
 *
 * @see FilterChain
 * @see Attribute
 *
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
public interface Filter {
    /**
     * Method is called, when the <tt>Filter</tt> has been added to the passed {@link FilterChain}.
     *
     * @param filterChain the {@link FilterChain} this <tt>Filter</tt> was added to.
     */
    void onAdded(FilterChain filterChain);

    /**
     * Method is called, when the <tt>Filter</tt> has been removed from the passed {@link FilterChain}.
     *
     * @param filterChain the {@link FilterChain} this <tt>Filter</tt> was removed from.
     */
    void onRemoved(FilterChain filterChain);

    /**
     * Method is called, when the {@link FilterChain} this <tt>Filter</tt> is part of, has been changed.
     *
     * @param filterChain the {@link FilterChain}.
     */
    void onFilterChainChanged(FilterChain filterChain);

    /**
     * Execute a unit of processing work to be performed, when channel will become available for reading. This
     * {@link Filter} may either complete the required processing and return false, or delegate remaining processing to the
     * next {@link Filter} in a {@link FilterChain} containing this {@link Filter} by returning true.
     * 
     * @param ctx {@link FilterChainContext}
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * @throws {@link java.io.IOException}
     */
    NextAction handleRead(FilterChainContext ctx) throws IOException;

    /**
     * Execute a unit of processing work to be performed, when some data should be written on channel. This {@link Filter}
     * may either complete the required processing and return false, or delegate remaining processing to the next
     * {@link Filter} in a {@link FilterChain} containing this {@link Filter} by returning true.
     * 
     * @param ctx {@link FilterChainContext}
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * @throws {@link java.io.IOException}
     */
    NextAction handleWrite(FilterChainContext ctx) throws IOException;

    /**
     * Execute a unit of processing work to be performed, when channel gets connected. This {@link Filter} may either
     * complete the required processing and return false, or delegate remaining processing to the next {@link Filter} in a
     * {@link FilterChain} containing this {@link Filter} by returning true.
     * 
     * @param ctx {@link FilterChainContext}
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * @throws {@link java.io.IOException}
     */
    NextAction handleConnect(FilterChainContext ctx) throws IOException;

    /**
     * Execute a unit of processing work to be performed, when server channel has accepted the client connection. This
     * {@link Filter} may either complete the required processing and return false, or delegate remaining processing to the
     * next {@link Filter} in a {@link FilterChain} containing this {@link Filter} by returning true.
     * 
     * @param ctx {@link FilterChainContext}
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * @throws {@link java.io.IOException}
     */
    NextAction handleAccept(FilterChainContext ctx) throws IOException;

    /**
     * Handle custom event associated with the {@link Connection}. This {@link Filter} may either complete the required
     * processing and return {@link StopAction}, or delegate remaining processing to the next {@link Filter} in a
     * {@link FilterChain} containing this {@link Filter} by returning {@link InvokeAction}.
     * 
     * @param ctx {@link FilterChainContext}
     * @param event
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * @throws {@link java.io.IOException}
     */
    NextAction handleEvent(FilterChainContext ctx, FilterChainEvent event) throws IOException;

    /**
     * Execute a unit of processing work to be performed, when connection has been closed. This {@link Filter} may either
     * complete the required processing and return false, or delegate remaining processing to the next {@link Filter} in a
     * {@link FilterChain} containing this {@link Filter} by returning true.
     * 
     * @param ctx {@link FilterChainContext}
     * @return {@link NextAction} instruction for {@link FilterChain}, how it should continue the execution
     * @throws {@link java.io.IOException}
     */
    NextAction handleClose(FilterChainContext ctx) throws IOException;

    /**
     * Notification about exception, occurred on the {@link FilterChain}
     *
     * @param ctx event processing {@link FilterChainContext}
     * @param error error, which occurred during <tt>FilterChain</tt> execution
     */
    void exceptionOccurred(FilterChainContext ctx, Throwable error);
}
