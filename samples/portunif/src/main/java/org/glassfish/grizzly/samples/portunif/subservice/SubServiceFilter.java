/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.portunif.subservice;

import java.io.IOException;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * The SUB-service {@link org.glassfish.grizzly.filterchain.Filter}, responsible for subtracting two values :)
 *
 * @author Alexey Stashok
 */
public class SubServiceFilter extends BaseFilter {
    final static byte[] magic = {'s', 'u', 'b'};
    

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

        // Get the input SubRequestMessage
        final SubRequestMessage subRequestMessage = ctx.getMessage();

        // Calculate the result
        final int result = subRequestMessage.getValue1() -
                subRequestMessage.getValue2();

        // Create the response message
        final SubResponseMessage subResponseMessage =
                new SubResponseMessage(result);

        // Send the response to the client
        ctx.write(subResponseMessage);
        
        // stop the filterchain processing
        return ctx.getStopAction();
    }
}
