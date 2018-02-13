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

package org.glassfish.grizzly.filterchain;

import java.io.IOException;
import java.util.List;
import org.glassfish.grizzly.*;

/**
 * <p>
 * This class implement the "Chain of Responsibility" pattern (for more info, 
 * take a look at the classic "Gang of Four" design patterns book). Towards 
 * that end, the Chain API models a computation as a series of "protocol filter"
 * that can be combined into a "protocol chain". 
 * </p><p>
 * The API for Filter consists of a two set of methods (handleXXX() and
 * postXXX) which is passed a "protocol context" parameter containing the
 * dynamic state of the computation, and whose return value is a
 * {@link NextAction} that instructs <tt>FilterChain</tt>, how it should
 * continue processing. The owning ProtocolChain  must call the
 * postXXX() method of each Filter in a FilterChain in reverse
 * order of the invocation of their handleXXX() methods.
 * </p><p>
 * The following picture describe how it Filter(s) 
 * </p><p><pre><code>
 * -----------------------------------------------------------------------------
 * - Filter1.handleXXX() --> Filter2.handleXXX()                    |          -
 * -                                                                |          -
 * -                                                                |          -
 * -                                                                |          -
 * - Filter1.postXXX() <-- Filter2.postXXX()                        |          -
 * -----------------------------------------------------------------------------
 * </code></pre></p><p>
 * The "context" abstraction is designed to isolate Filter
 * implementations from the environment in which they are run 
 * (such as a Filter that can be used in either IIOP or HTTP parsing, 
 * without being tied directly to the API contracts of either of these 
 * environments). For Filter that need to allocate resources prior to 
 * delegation, and then release them upon return (even if a delegated-to 
 * Filter throws an exception), the "postXXX" method can be used
 * for cleanup. 
 * </p>
 *
 * @see Filter
 * @see Codec
 *
 * @author Jeanfrancois Arcand
 * @author Alexey Stashok
 */
public interface FilterChain extends Processor<Context>, List<Filter> {
    FilterChainContext obtainFilterChainContext(Connection connection);
    FilterChainContext obtainFilterChainContext(Connection connection, Closeable closeable);
    FilterChainContext obtainFilterChainContext(Connection connection,
            int startIdx, int endIdx, int currentIdx);
    FilterChainContext obtainFilterChainContext(Connection connection,
            Closeable closeable,
            int startIdx, int endIdx, int currentIdx);

    /**
     * Get the index of {@link Filter} in chain, which type is filterType, or
     * <tt>-1</tt> if the {@link Filter} of required type was not found.
     * 
     * @param filterType the type of {@link Filter} to search.
     * @return the index of {@link Filter} in chain, which type is filterType, or
     * <tt>-1</tt> if the {@link Filter} of required type was not found.
     */
    int indexOfType(final Class<? extends Filter> filterType);

    /**
     * Method processes occurred {@link IOEvent} on this {@link FilterChain}.
     *
     * @param context processing context
     * @return {@link ProcessorResult}
     */
    ProcessorResult execute(FilterChainContext context);

    void flush(Connection connection,
            CompletionHandler<WriteResult> completionHandler);

    void fireEventUpstream(Connection connection,
            FilterChainEvent event,
            CompletionHandler<FilterChainContext> completionHandler);
    
    void fireEventDownstream(Connection connection,
            FilterChainEvent event,
            CompletionHandler<FilterChainContext> completionHandler);

    ReadResult read(FilterChainContext context) throws IOException;

    void fail(FilterChainContext context, Throwable failure);
}
