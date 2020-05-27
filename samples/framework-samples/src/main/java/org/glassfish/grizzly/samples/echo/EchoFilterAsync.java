/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.echo;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Implementation of {@link FilterChain} filter, which asynchronously replies with the request message.
 *
 * @author Alexey Stashok
 */
public class EchoFilterAsync extends BaseFilter {

    // Create Scheduled thread pool
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5, new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }
    });

    /**
     * Handle just read operation, when some message has come and ready to be processed.
     *
     * @param ctx Context of {@link FilterChainContext} processing
     * @return the next action
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {

        final Object message = ctx.getMessage();

        // If message is not null - it's first time the filter is getting called
        // and we need to init async thread, which will reply

        // Peer address is used for non-connected UDP Connection :)
        final Object peerAddress = ctx.getAddress();

        // Get the SuspendAction in advance, cause once we execute LongLastTask in the
        // custom thread - we lose control over the context
        final NextAction suspendActipn = ctx.getSuspendAction();

        // suspend the current execution
        ctx.suspend();

        // schedule async work
        scheduler.schedule(new Runnable() {

            @Override
            public void run() {
                // write the response
                ctx.write(peerAddress, message, null);

                // resume the context
                ctx.resumeNext();
            }
        }, 5, TimeUnit.SECONDS);

        // return suspend status
        return suspendActipn;
    }

}
