/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.utils;

import java.io.IOException;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

/**
 * Echo {@link Filter} implementation
 *
 * @author Alexey Stashok
 */
public class EchoFilter extends BaseFilter {

    private static final Logger logger = Grizzly.logger(EchoFilter.class);

    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final Object message = ctx.getMessage();
        final Connection connection = ctx.getConnection();
        final Object address = ctx.getAddress();

        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "EchoFilter. connection={0} dstAddress={1} message={2}", new Object[] { connection, address, message });
        }

        if (message instanceof Buffer) {
            ((Buffer) message).allowBufferDispose(true);
        }

        ctx.write(address, message, null);

        return ctx.getStopAction();
    }
}
