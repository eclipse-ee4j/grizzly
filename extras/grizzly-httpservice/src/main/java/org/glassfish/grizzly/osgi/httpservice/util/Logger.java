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

package org.glassfish.grizzly.osgi.httpservice.util;

import org.osgi.util.tracker.ServiceTracker;
import org.osgi.service.log.LogService;

/**
 * Logger Utility.
 *
 * @author Hubert Iwaniuk
 * @since Jan 20, 2009
 */
public class Logger {
    private final ServiceTracker logTracker;

    public Logger(final ServiceTracker logTracker) {
        this.logTracker = logTracker;
    }

    private void log(final int logLevel, final String msg) {
        LogService log = (LogService) logTracker.getService();
        if (log == null) {
            if (logLevel < LogService.LOG_WARNING)
                System.out.println(msg);
        } else {
            log.log(logLevel, msg);
        }
    }

    private void log(final int logLevel, final String msg, final Throwable e) {
        LogService log = (LogService) logTracker.getService();
        if (log == null) {
            System.out.println(msg);
            e.printStackTrace(System.out);
        } else {
            log.log(logLevel, msg, e);
        }
    }

    public void info(String msg) {
        log(LogService.LOG_INFO, msg);
    }

    public void debug(String msg) {
        log(LogService.LOG_DEBUG, msg);
    }

    public void warn(String msg) {
        log(LogService.LOG_WARNING, msg);
    }

    public void warn(String msg, Throwable e) {
        log(LogService.LOG_WARNING, msg, e);
    }

    public void error(String msg, Throwable e) {
        log(LogService.LOG_ERROR, msg, e);
    }
}
