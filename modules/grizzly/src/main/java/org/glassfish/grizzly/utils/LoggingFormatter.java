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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * Format the record to include the Thread that logged the record.
 * To change the default configuration for java.util.logging you will need to
 * add this in the command line parameters : -Djava.util.logging.config.file=myfile
 *
 * Here a sample of what you need to include in myfile
 *
 * #the default logger is this add you can replace it with LoggingFormatter
 * #java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
 * java.util.logging.ConsoleHandler.formatter = com.glassfish.grizzly.utils.LoggingFormatter
 *
 * refer to : https://grizzly.dev.java.net/issues/show_bug.cgi?id=291
 *
 * @author Sebastien Dionne
 *
 */
public class LoggingFormatter extends Formatter {

    private static final Logger log = Logger.getLogger(LoggingFormatter.class.getName());
    // took that from the JDK java.util.logging.SimpleFormatter
    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private static String lineSeparator = "\n";

    static {
        try {
            String separator = System.getProperty("line.separator");

            if (separator != null && separator.trim().length() > 0) {
                lineSeparator = separator;
            }
        } catch (SecurityException se) {
            // ignore the exception
        }

    }

    public LoggingFormatter() {
        super();
    }

    /**
     * Format the record to include the Thread that logged this record.
     * the format should be
     * [WorkerThreadImpl-1, Grizzly] 2008-10-08 18:49:59 [INFO] com.glassfish.grizzly.Controller:doSelect message
     *
     * @param record The record to be logged into the logger.
     *
     * @return the record formated to be more human readable
     */
    @Override
    public String format(LogRecord record) {

        // Create a StringBuffer to contain the formatted record
        StringBuffer sb = new StringBuffer(128);

        sb.append('[').append(Thread.currentThread().getName()).append("] ");

        // Get the date from the LogRecord and add it to the buffer
        Date date = new Date(record.getMillis());
        sb.append(date.toString()).append(' ');

        // Get the level name and add it to the buffer
        sb.append('[').append(record.getLevel().getLocalizedName()).append("] ");

        // Get Class name
        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        } else {
            sb.append(record.getLoggerName());
        }
        // Get method name
        if (record.getSourceMethodName() != null) {
            sb.append(' ');
            sb.append(record.getSourceMethodName());
        }
        sb.append(':').append(lineSeparator);

        // Get the formatted message (includes localization
        // and substitution of parameters) and add it to the buffer
        sb.append(formatMessage(record)).append(lineSeparator);

        //we log the stackTrace if it's a exception
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ignored) {
            }
        }
        sb.append(lineSeparator);

        return sb.toString();
    }

    /**
     * Example to test the com.glassfish.grizzly.utils.LoggingFormatter
     * You need to include this parameter in the command line
     * -Djava.util.logging.config.file=myfile
     * @param args main parameters
     */
    public static void main(String[] args) {

        log.info("Info Event");

        log.severe("Severe Event");

        // show the thread info in the logger.
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                log.info("Info Event in Thread");
            }
        }, "Thread into main");

        t.start();

        log.log(Level.SEVERE, "exception", new Exception());
    }
}
