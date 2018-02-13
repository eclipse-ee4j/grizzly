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
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * The SUB-service message parser/serializer, which is responsible for parsing
 * {@link SubResponseMessage} and serializing {@link SubRequestMessage}.
 *
 * @author Alexey Stashok
 */
public class SubClientMessageFilter extends BaseFilter {
    private final static int MESSAGE_SIZE = 4;  // BODY = RESULT(INT) = 4

    /**
     * Handle just read operation, when some message has come and ready to be
     * processed.
     *
     * @param ctx Context of {@link FilterChainContext} processing
     * @return the next action
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        // Take input buffer
        final Buffer input = ctx.getMessage();

        // If the available data is not enough to parse the message - stop
        if (input.remaining() < MESSAGE_SIZE) {
            return ctx.getStopAction(input);
        }

        // Read result
        final int result = input.getInt(0);

        // Construct SubResponseMessage, based on the result
        final SubResponseMessage subResponseMessage =
                new SubResponseMessage(result);
        // set the SubResponseMessage on context
        ctx.setMessage(subResponseMessage);

        // Split the remainder, if any
        final Buffer remainder = input.remaining() > MESSAGE_SIZE ?
            input.split(MESSAGE_SIZE) : null;

        // Try to dispose the parsed chunk
        input.tryDispose();
        
        // continue filter chain execution
        return ctx.getInvokeAction(remainder);
    }

    /**
     * Method is called, when we write a data to the Connection.
     *
     * We override this method to perform SubRequestMessage -> Buffer transformation.
     *
     * @param ctx Context of {@link FilterChainContext} processing
     * @return the next action
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        // Take the source SubRequestMessage
        final SubRequestMessage subRequestMessage = ctx.getMessage();

        final int value1 = subRequestMessage.getValue1();
        final int value2 = subRequestMessage.getValue2();

        // Get MemoryManager
        final MemoryManager mm = ctx.getConnection().getTransport().getMemoryManager();
        // Allocate the Buffer
        final Buffer output = mm.allocate(11);
        // Add ADD-service magic
        output.put(SubServiceFilter.magic);
        // Add value1
        output.putInt(value1);
        // Add value2
        output.putInt(value2);
        
        // Allow Grizzly dispose this Buffer
        output.allowBufferDispose();

        // Set the Buffer to the context
        ctx.setMessage(output.flip());

        // continue filterchain execution
        return ctx.getInvokeAction();
    }

}
