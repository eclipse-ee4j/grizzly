/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.udpmulticast;

import java.io.IOException;
import java.util.Date;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Simple Filter responsible for printing incoming message to {@link System#out}.
 * 
 * @author Alexey Stashok
 */
public class PrintFilter extends BaseFilter {
    /**
     * {@inheritDoc}
     */
    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        final String message = ctx.getMessage();
        System.out.println(new Date() + " " + ctx.getAddress() + ": " + message);
        
        return ctx.getStopAction();
    }
}
