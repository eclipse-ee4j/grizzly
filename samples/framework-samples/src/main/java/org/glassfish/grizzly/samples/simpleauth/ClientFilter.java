/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.simpleauth;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Simple filter, which prints out the server echo message.
 *
 * @author Alexey Stashok
 */
public class ClientFilter extends BaseFilter {
    private final static Logger logger = Grizzly.logger(ClientFilter.class);
    
    /**
     * The method is called, when we receive a message from a server.
     * 
     * @param ctx Request processing context
     *
     * @return {@link NextAction}
     * @throws IOException
     */
    @Override
    public NextAction handleRead(FilterChainContext ctx)
            throws IOException {
        // Get the message
        final MultiLinePacket message = ctx.getMessage();
        
        logger.info("---------Client got a response:\n" + message);
        
        return ctx.getStopAction();
    }

}
