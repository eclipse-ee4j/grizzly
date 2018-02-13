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

import java.io.Closeable;
import java.io.IOException;

/**
 * An interface defining an <em>appender</em> for Grizzly access logs entries.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 * @author <a href="http://www.usrz.com/">USRZ.com</a>
 */
public interface AccessLogAppender extends Closeable {

    /**
     * Append the specified access log entry.
     *
     * @param accessLogEntry The {@link String} value of the data to be append
     *                       in the access log.
     * @throws IOException If an I/O error occurred appending to the log.
     */
    void append(String accessLogEntry)
    throws IOException;

    /**
     * Close any underlying resource owned by this appender.
     */
    @Override
    void close()
    throws IOException;
}
