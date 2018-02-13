/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Filter, which determines silent connections and closes them.
 * The silent connection is a connection, which didn't send/receive any byte
 * since it was accepted during specified period of time.
 * 
 * @author Alexey Stashok
 */
public final class SilentConnectionFilter extends BaseFilter {
    private static final Logger LOGGER = Grizzly.logger(SilentConnectionFilter.class);

    public static final long UNLIMITED_TIMEOUT = -1;
    public static final long UNSET_TIMEOUT = 0;

    private static final String ATTR_NAME =
            SilentConnectionFilter.class.getName() + ".silent-connection-attr";

    private static final Attribute<Long> silentConnectionAttr =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(ATTR_NAME);

    private final long timeoutMillis;
    private final DelayedExecutor.DelayQueue<Connection> queue;

    public SilentConnectionFilter(DelayedExecutor executor,
            long timeout, TimeUnit timeunit) {
        this.timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, timeunit);
        queue = executor.createDelayQueue(
                new DelayedExecutor.Worker<Connection>() {

            @Override
            public boolean doWork(Connection connection) {
                connection.closeSilently();
                return true;
            }
        }, new Resolver());
    }

    public long getTimeout(TimeUnit timeunit) {
        return timeunit.convert(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public NextAction handleAccept(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        queue.add(connection, timeoutMillis, TimeUnit.MILLISECONDS);

        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        queue.remove(connection);
        
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        queue.remove(connection);

        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        queue.remove(ctx.getConnection());
        return ctx.getInvokeAction();
    }

    private static final class Resolver implements DelayedExecutor.Resolver<Connection> {

        @Override
        public boolean removeTimeout(Connection connection) {
            return silentConnectionAttr.remove(connection) != null;
        }

        @Override
        public long getTimeoutMillis(Connection connection) {
            final Long timeout = silentConnectionAttr.get(connection);
            return timeout != null ? timeout : DelayedExecutor.UNSET_TIMEOUT;
        }

        @Override
        public void setTimeoutMillis(Connection connection, long timeoutMillis) {
            silentConnectionAttr.set(connection, timeoutMillis);
        }
    }
}
