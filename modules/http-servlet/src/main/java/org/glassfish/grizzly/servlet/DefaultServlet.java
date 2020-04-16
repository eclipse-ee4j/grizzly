/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.StaticHttpHandlerBase;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This servlet will be invoked when no other servlet matches the request URI.
 *
 * TODO:  This needs more work.  Review the DefaultServlet implementations
 *   included with popular servlet containers to get an understanding of
 *   what may be added
 *
 *   @since 2.2
 */
public class DefaultServlet extends HttpServlet {

    private final StaticHttpHandlerBase staticHttpHandlerBase;

    // ------------------------------------------------------------ Constructors


    protected DefaultServlet(final StaticHttpHandlerBase staticHttpHandlerBase) {

        this.staticHttpHandlerBase = staticHttpHandlerBase;

    }


    // ------------------------------------------------ Methods from HttpServlet


    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final Request request = unwrap(req).getRequest();
        try {
            staticHttpHandlerBase.service(request, request.getResponse());
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private static HttpServletRequestImpl unwrap(final ServletRequest request) {
        return ((request instanceof  HttpServletRequestImpl)
                    ? (HttpServletRequestImpl) request
                    : unwrap(((ServletRequestWrapper) request).getRequest()));
    }

}
