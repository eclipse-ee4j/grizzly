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

package org.glassfish.grizzly.asyncqueue;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Context;
import org.glassfish.grizzly.ProcessorResult;

/**
 * Common interface for {@link AsyncQueue} processors.
 * 
 * @author Alexey Stashok
 */
public interface AsyncQueue {
    String EXPECTING_MORE_OPTION =
            AsyncQueue.class.getName() + ".expectingMore";
    
    enum AsyncResult {
        COMPLETE(ProcessorResult.createLeave()),
        INCOMPLETE(ProcessorResult.createComplete()),
        EXPECTING_MORE(ProcessorResult.createComplete(EXPECTING_MORE_OPTION)),
        TERMINATE(ProcessorResult.createTerminate());

        private final ProcessorResult result;
        
        AsyncResult(final ProcessorResult result) {
            this.result = result;
        }

        public ProcessorResult toProcessorResult() {
            return result;
        }
        
    }
    
    /**
     * Checks whether there is ready data in {@link AsyncQueue},
     * associated with the {@link Connection}.
     * 
     * @param connection {@link Connection}
     * @return <tt>true</tt>, if there is ready data,
     *         or <tt>false</tt> otherwise.
     */
    boolean isReady(Connection connection);
    
    /**
     * Callback method, which is called async. to process ready
     * {@link AsyncQueue}, which are associated with the given
     * {@link Connection}
     * 
     * @param context {@link Context}
     * @return {@link AsyncResult}, depending on async queue status.
     */
    AsyncResult processAsync(Context context);
    
    /**
     * Callback method, which is called, when {@link Connection} has been closed,
     * to let processor release a connection associated resources.
     * 
     * @param connection {@link Connection}
     */
    void onClose(Connection connection);
    
    /**
     * Close <tt>AsyncQueueProcessor</tt> and release associated resources
     */
    void close();
}
