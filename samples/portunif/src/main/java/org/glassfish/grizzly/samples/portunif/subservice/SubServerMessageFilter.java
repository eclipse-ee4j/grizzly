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
 * {@link SubRequestMessage} and serializing {@link SubResponseMessage}.
 *
 * @author Alexey Stashok
 */
public class SubServerMessageFilter extends BaseFilter {
    private final static int MESSAGE_MAGIC_SIZE = 3;
    private final static int MESSAGE_SIZE = MESSAGE_MAGIC_SIZE + 8;  // BODY = VALUE1(INT) + VALUE2(INT) = 8

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

        // Read value1
        final int value1 = input.getInt(MESSAGE_MAGIC_SIZE);
        // Read value2
        final int value2 = input.getInt(MESSAGE_MAGIC_SIZE + 4);

        // Construct SubRequestMessage, based on the value1, value2
        final SubRequestMessage subRequestMessage =
                new SubRequestMessage(value1, value2);
        // set the SubRequestMessage on context
        ctx.setMessage(subRequestMessage);

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
     * We override this method to perform SubResponseMessage -> Buffer transformation.
     *
     * @param ctx Context of {@link FilterChainContext} processing
     * @return the next action
     * @throws java.io.IOException
     */
    @Override
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        // Take the source SubResponseMessage
        final SubResponseMessage subResponseMessage = ctx.getMessage();

        final int result = subResponseMessage.getResult();

        // Get MemoryManager
        final MemoryManager mm = ctx.getConnection().getTransport().getMemoryManager();
        // Allocate the Buffer
        final Buffer output = mm.allocate(4);
        // Add result
        output.putInt(result);

        // Allow Grizzly dispose this Buffer
        output.allowBufferDispose();

        // Set the Buffer to the context
        ctx.setMessage(output.flip());

        // continue filterchain execution
        return ctx.getInvokeAction();
    }

}
