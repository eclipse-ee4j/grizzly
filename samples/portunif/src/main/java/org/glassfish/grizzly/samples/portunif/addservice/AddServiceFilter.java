/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.portunif.addservice;

import java.io.IOException;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * The ADD-service {@link org.glassfish.grizzly.filterchain.Filter}, responsible for adding two values :)
 * 
 * @author Alexey Stashok
 */
public class AddServiceFilter extends BaseFilter {
    final static byte[] magic = {'a', 'd', 'd'};


    /**
     * Handle just read operation, when some message has come and ready to be
     * processed.
     *
     * @param ctx Context of {@link FilterChainContext} processing
     * @return the next action
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleRead(final FilterChainContext ctx)
            throws IOException {

        // Get the input AddRequestMessage
        final AddRequestMessage addRequestMessage = ctx.getMessage();

        // Calculate the result
        final int result = addRequestMessage.getValue1() +
                addRequestMessage.getValue2();

        // Create the response message
        final AddResponseMessage addResponseMessage =
                new AddResponseMessage(result);

        // Send the response to the client
        ctx.write(addResponseMessage);

        // stop the filterchain processing
        return ctx.getStopAction();
    }
}
