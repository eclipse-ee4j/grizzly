/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple log {@link Filter}
 * 
 * @author Alexey Stashok
 */
public class LogFilter extends BaseFilter {
    private final Logger logger;
    private final Level level;

    public LogFilter() {
        this(null, null);
    }

    public LogFilter(Logger logger) {
        this(logger, null);
    }

    public LogFilter(Logger logger, Level level) {
        if (logger != null) {
            this.logger = logger;
        } else {
            this.logger = Grizzly.logger(LogFilter.class);
        }

        if (level != null) {
            this.level = level;
        } else {
            this.level = Level.INFO;
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public Level getLevel() {
        return level;
    }

    @Override
    public void onAdded(FilterChain filterChain) {
        logger.log(level, "LogFilter onAdded");
    }

    @Override
    public void onRemoved(FilterChain filterChain) {
        logger.log(level, "LogFilter onRemoved");
    }

    @Override
    public void onFilterChainChanged(FilterChain filterChain) {
        logger.log(level, "LogFilter onFilterChainChanged");
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        logger.log(level, "LogFilter handleRead. Connection={0} message={1}",
                new Object[] {ctx.getConnection(), ctx.getMessage()});
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        logger.log(level, "LogFilter handleWrite. Connection={0} message={1}",
                new Object[] {ctx.getConnection(), ctx.getMessage()});
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        logger.log(level, "LogFilter handleConnect. Connection={0} message={1}",
                new Object[] {ctx.getConnection(), ctx.getMessage()});
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleAccept(FilterChainContext ctx) throws IOException {
        logger.log(level, "LogFilter handleAccept. Connection={0} message={1}",
                new Object[] {ctx.getConnection(), ctx.getMessage()});
        return ctx.getInvokeAction();
    }

    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        logger.log(level, "LogFilter handleClose. Connection={0} message={1}",
                new Object[] {ctx.getConnection(), ctx.getMessage()});
        return ctx.getInvokeAction();
    }

    @Override
    public void exceptionOccurred(FilterChainContext ctx,
            Throwable error) {
        logger.log(level, "LogFilter exceptionOccured. Connection={0} message={1}",
                new Object[] {ctx.getConnection(), ctx.getMessage()});
    }
}
