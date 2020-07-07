/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * An {@link AccessLogAppender appender} writing log entries to an {@link OutputStream}.
 *
 * <p>
 * Log entries will <b>always</b> encoded in <em>UTF-8</em>.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 * @author <a href="http://www.usrz.com/">USRZ.com</a>
 */
public class StreamAppender implements AccessLogAppender {

    /* Line separator for entries, respect Windoshhhh */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    /* The writer we'll actually use */
    private final Writer writer;

    /**
     * Create a new {@link StreamAppender} instance writing log entries to the specified {@link OutputStream}.
     */
    public StreamAppender(OutputStream output) {
        writer = new OutputStreamWriter(output, Charset.forName("UTF-8"));
    }

    @Override
    public void append(String accessLogEntry) throws IOException {
        synchronized (this) {
            writer.write(accessLogEntry);
            writer.write(LINE_SEPARATOR);
            writer.flush();
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

}
