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

package org.glassfish.grizzly.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.Set;
import org.glassfish.grizzly.CompletionHandler;

/**
 *
 * @author Alexey Stashok
 */
public interface SelectorHandler {

    /**
     * The default {@link SelectorHandler} used by all created builder instances.
     */
    SelectorHandler DEFAULT_SELECTOR_HANDLER = new DefaultSelectorHandler();

    long getSelectTimeout();

    boolean preSelect(SelectorRunner selectorRunner) throws IOException;
    
    Set<SelectionKey> select(SelectorRunner selectorRunner) throws IOException;
    
    void postSelect(SelectorRunner selectorRunner) throws IOException;
    
    void registerKeyInterest(SelectorRunner selectorRunner,
                             SelectionKey key,
                             int interest)
    throws IOException;
    
    /**
     * Deregisters SelectionKey interest.
     * 
     * This method must be called from the SelectorRunner's thread only!
     * @throws IOException 
     */
    void deregisterKeyInterest(SelectorRunner selectorRunner,
                               SelectionKey key,
                               int interest)
    throws IOException;

    void registerChannel(SelectorRunner selectorRunner,
                         SelectableChannel channel,
                         int interest,
                         Object attachment)
    throws IOException;

    void registerChannelAsync(
                                    SelectorRunner selectorRunner,
                                    SelectableChannel channel,
                                    int interest,
                                    Object attachment,
                                    CompletionHandler<RegisterChannelResult> completionHandler);

    /**
     * Deregister the channel from the {@link SelectorRunner}'s Selector.
     * @param selectorRunner {@link SelectorRunner}
     * @param channel {@link SelectableChannel} channel to deregister
     * @throws IOException
     */
    void deregisterChannel(SelectorRunner selectorRunner,
                           SelectableChannel channel) throws IOException;

    /**
     * Deregister the channel from the {@link SelectorRunner}'s Selector.
     * @param selectorRunner {@link SelectorRunner}
     * @param channel {@link SelectableChannel} channel to deregister
     * @param completionHandler {@link CompletionHandler}
     */
    void deregisterChannelAsync(
                                    SelectorRunner selectorRunner,
                                    SelectableChannel channel,
                                    CompletionHandler<RegisterChannelResult> completionHandler);


    /**
     * Execute task in a selector thread.
     * Unlike {@link #enque(org.glassfish.grizzly.nio.SelectorRunner, org.glassfish.grizzly.nio.SelectorHandler.Task, org.glassfish.grizzly.CompletionHandler)},
     * this operation will execute the task immediately if the current
     * is a selector thread.
     * 
     * @param selectorRunner
     * @param task
     * @param completionHandler
     */
    void execute(
                                    final SelectorRunner selectorRunner,
                                    final Task task,
                                    final CompletionHandler<Task> completionHandler);

    /**
     * Execute task in a selector thread.
     * Unlike {@link #execute(org.glassfish.grizzly.nio.SelectorRunner, org.glassfish.grizzly.nio.SelectorHandler.Task, org.glassfish.grizzly.CompletionHandler)},
     * this operation will postpone the task execution if current thread
     * is a selector thread, and execute it during the next
     * {@link #select(org.glassfish.grizzly.nio.SelectorRunner)} iteration.
     * 
     * @param selectorRunner
     * @param task
     * @param completionHandler
     */
    void enque(
                                    final SelectorRunner selectorRunner,
                                    final Task task,
                                    final CompletionHandler<Task> completionHandler);

    boolean onSelectorClosed(SelectorRunner selectorRunner);
    
    interface Task {
        boolean run() throws Exception;
    }        
}
