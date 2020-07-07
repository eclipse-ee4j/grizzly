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

package org.glassfish.grizzly.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.http.server.Request;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.WebConnection;

/**
 * Implementation of WebConnection for Servlet 3.1
 *
 * @author Amy Roh
 * @author Shing Wai Chan
 * @version $Revision: 1.23 $ $Date: 2007/07/09 20:46:45 $
 */
public class WebConnectionImpl implements WebConnection {

    private final ServletInputStream inputStream;

    private final ServletOutputStream outputStream;

    private final HttpServletRequestImpl request;

    private final AtomicBoolean isClosed = new AtomicBoolean();

    // ----------------------------------------------------------- Constructor

    public WebConnectionImpl(HttpServletRequestImpl request, ServletInputStream inputStream, ServletOutputStream outputStream) {
        this.request = request;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    /**
     * Returns an input stream for this web connection.
     *
     * @return a ServletInputStream for reading binary data
     *
     * @exception java.io.IOException if an I/O error occurs
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        return inputStream;
    }

    /**
     * Returns an output stream for this web connection.
     *
     * @return a ServletOutputStream for writing binary data
     *
     * @exception IOException if an I/O error occurs
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public void close() throws Exception {
        // Make sure we run close logic only once
        if (isClosed.compareAndSet(false, true)) {
            final Request grizzlyRequest = request.getRequest();

            HttpUpgradeHandler httpUpgradeHandler = request.getHttpUpgradeHandler();
            try {
                httpUpgradeHandler.destroy();
            } finally {
                try {
                    inputStream.close();
                } catch (Exception ignored) {
                }
                try {
                    outputStream.close();
                } catch (Exception ignored) {
                }

                grizzlyRequest.getResponse().resume();
            }
        }
    }
}
