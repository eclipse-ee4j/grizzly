/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.util;

import java.io.IOException;

import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * AggregatorAddOn installs {@link AggregatorFilter} into HttpServer FilterChain. {@link AggregatorFilter} is
 * responsible for aggregating input HTTP message payload (either request or response) and pass it to the next filter in
 * chain only when entire HTTP message (including payload) is read.
 *
 * @author Alexey Stashok
 */
public class AggregatorAddOn implements AddOn {

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(final NetworkListener networkListener, final FilterChainBuilder builder) {

        // Get the index of HttpServerFilter in the HttpServer filter chain
        final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

        if (httpServerFilterIdx >= 0) {
            // Insert the AggregatorFilter right before HttpServerFilter
            builder.add(httpServerFilterIdx, new AggregatorFilter());
        }
    }

    private static class AggregatorFilter extends BaseFilter {

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final Object message = ctx.getMessage();

            // If the input message is not HttpContent or it's last HttpContent message -
            // pass the message to a next filter
            if (!(message instanceof HttpContent) || ((HttpContent) message).isLast()) {
                return ctx.getInvokeAction();
            }

            // if it's HttpContent chunk (not last) - save it and stop filter chain processing.
            return ctx.getStopAction(message);
        }
    }
}
