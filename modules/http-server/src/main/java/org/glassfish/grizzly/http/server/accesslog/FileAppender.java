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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpServer;

/**
 * An {@link AccessLogAppender appender} writing log entries to {@link File}s.
 *
 * @author <a href="mailto:pier@usrz.com">Pier Fumagalli</a>
 * @author <a href="http://www.usrz.com/">USRZ.com</a>
 */
public class FileAppender extends StreamAppender {

    private static final Logger LOGGER = Grizzly.logger(HttpServer.class);

    /**
     * Create a new {@link FileAppender} <em>appending to</em> (and not overwriting) the specified {@link File}.
     *
     * @throws IOException If an I/O error occurred opening the file.
     */
    public FileAppender(File file) throws IOException {
        this(file, true);
    }

    /**
     * Create a new {@link FileAppender} writing to the specified {@link File}.
     *
     * @param append If <b>true</b> the file will be <em>appended to</em>, otherwise it will be completely
     * <em>overwritten</em>.
     * @throws IOException If an I/O error occurred opening the file.
     */
    public FileAppender(File file, boolean append) throws IOException {
        super(new FileOutputStream(file, append));
        LOGGER.info("Access log file \"" + file.getAbsolutePath() + "\" opened");
    }
}
