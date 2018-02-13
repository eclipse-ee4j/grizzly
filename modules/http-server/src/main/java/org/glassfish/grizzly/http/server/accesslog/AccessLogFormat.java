/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.accesslog;

import java.util.Date;

import org.glassfish.grizzly.http.server.Response;

/**
 * An interface defining a component capable of formatting {@link Response}s
 * into printable <em>access log entries</em>.
 *
 * <p>Implementations of this class <b>must</b> be thread-safe.</p>
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 * @author <a href="http://www.usrz.com/">USRZ.com</a>
 */
public interface AccessLogFormat {

    /**
     * Format the data contained in the specified {@link Response} and return
     * a {@link String} which can be appended to an access log file.
     *
     * @param response The {@link Response} holding the data to format.
     * @param timeStamp The {@link Date} at which the request was originated.
     * @param responseNanos The time, in nanoseconds, the {@link Response}
     *                      took to complete.
     */
    String format(Response response, Date timeStamp, long responseNanos);

}
