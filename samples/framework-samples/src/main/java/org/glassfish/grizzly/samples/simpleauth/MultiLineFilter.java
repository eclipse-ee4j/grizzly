/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.simpleauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * The {@link org.glassfish.grizzly.filterchain.Filter} is responsible for a {@link List&lt;String&gt;} <->
 * {@link MultiLinePacket} transformations.
 *
 * When reading - filter is gathering {@link String} lines into a MultiLinePacket, when writing - filter breaks
 * {@link MultiLinePacket} into {@link String} list.
 *
 * @author Alexey Stashok
 */
public class MultiLineFilter extends BaseFilter {
    private static final Logger LOGGER = Grizzly.logger(MultiLineFilter.class);

    // MultiLinePacket terminating line (the String line value, which indicates last line in a MultiLinePacket}.
    private final String terminatingLine;

    // Attribute to store the {@link String} list decoding state.
    private static final Attribute<MultiLinePacket> incompletePacketAttr = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute("Multiline-decoder-packet");

    public MultiLineFilter(String terminatingLine) {
        this.terminatingLine = terminatingLine;
    }

    /**
     * The method is called once we have received a single {@link String} line.
     *
     * Filter check if it's {@link MultiLinePacket} terminating line, if yes - we assume {@link MultiLinePacket} completed
     * and pass control to a next {@link org.glassfish.grizzly.filterchain.Filter} in a chain. If it's not a terminating
     * line - we add another string line to a {@link MultiLinePacket} and stop the request processing until more strings
     * will get available.
     *
     * @param ctx Request processing context
     *
     * @return {@link NextAction}
     * @throws IOException
     */
    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        // Get the Connection
        final Connection connection = ctx.getConnection();

        // Get parsed String
        List<String> input = ctx.getMessage();

        MultiLinePacket packet = incompletePacketAttr.remove(connection);
        if (packet == null) {
            packet = MultiLinePacket.create();
        }

        boolean foundTerm = false;
        for (Iterator<String> it = input.iterator(); it.hasNext();) {
            final String line = it.next();
            it.remove();

            if (line.equals(terminatingLine)) {
                foundTerm = true;
                break;
            }

            packet.getLines().add(line);
        }

        if (!foundTerm) {
            incompletePacketAttr.set(connection, packet);
            return ctx.getStopAction();
        }

        // Set MultiLinePacket packet as a context message
        ctx.setMessage(packet);
        LOGGER.log(Level.INFO, "-------- Received from network:\n{0}", packet);

        return input.isEmpty() ? ctx.getInvokeAction() : ctx.getInvokeAction(input);

    }

    /**
     * The method is called when we send MultiLinePacket.
     *
     * @param ctx Request processing context
     *
     * @return {@link NextAction}
     * @throws IOException
     */
    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {

        // Get a processing MultiLinePacket
        final MultiLinePacket input = ctx.getMessage();

        LOGGER.log(Level.INFO, "------- Sending to network:\n{0}", input);

        // pass MultiLinePacket as List<String>.
        // we could've used input.getLines() as collection to be passed
        // downstream, but we don't want to modify it (adding and removing terminatingLine).
        final List<String> stringList = new ArrayList<>(input.getLines().size() + 1);
        stringList.addAll(input.getLines());
        stringList.add(terminatingLine);

        ctx.setMessage(stringList);

        return ctx.getInvokeAction();
    }
}
