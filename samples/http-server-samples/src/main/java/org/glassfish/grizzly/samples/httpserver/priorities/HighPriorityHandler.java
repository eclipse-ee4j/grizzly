/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.httpserver.priorities;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.RequestExecutorProvider;
import org.glassfish.grizzly.http.server.Response;

/**
 * The high-priority {@link HttpHandler} we want to be processed as fast as possible.
 * 
 * @author Alexey Stashok
 */
public class HighPriorityHandler extends HttpHandler {

    private final RequestExecutorProvider executorProvider;
    
    public HighPriorityHandler(final ExecutorService threadPool) {
        this.executorProvider = new RequestExecutorProvider() {

            @Override
            public Executor getExecutor(Request request) {
                return threadPool;
            }
        };
    }
    
    /**
     * @return the {@link RequestExecutorProvider} to be used to 
     * call {@link HighPriorityHandler#service(org.glassfish.grizzly.http.server.Request, org.glassfish.grizzly.http.server.Response)}.
     */
    @Override
    public RequestExecutorProvider getRequestExecutorProvider() {
        return executorProvider;
    }

    @Override
    public void service(final Request request, final Response response)
            throws Exception {
        response.getWriter().write(Thread.currentThread().getName() + ": executing high priority task");
    }    
}
