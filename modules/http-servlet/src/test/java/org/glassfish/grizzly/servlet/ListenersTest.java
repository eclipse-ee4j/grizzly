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
import java.net.HttpURLConnection;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ListenersTest extends HttpServerAbstractTest {

    public static final int PORT = 18088;

    // ------------------------------------------------------------ Test Methods

    /**
     * Regression test for GRIZZLY-1218 and GRIZZLY-1220.
     */
    public void testRequestListener() throws Exception {
        newHttpServer(PORT);
        WebappContext ctx = new WebappContext("Test", "/contextPath");
        ctx.addListener(RequestListener.class.getName());
        addServlet(ctx, "TestServlet", "/servletPath/*");
        ctx.deploy(httpServer);
        httpServer.start();
        HttpURLConnection conn = getConnection("/contextPath/servletPath/pathInfo", PORT);
        conn.getResponseCode();
        assertTrue(RequestListener.destroyed);
        assertTrue(RequestListener.initialized);

    }

    // --------------------------------------------------------- Private Methods

    private ServletRegistration addServlet(final WebappContext ctx, final String name, final String alias) {
        final ServletRegistration reg = ctx.addServlet(name, new HttpServlet() {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setHeader("Path-Info", req.getPathInfo());
                resp.setHeader("Request-Was", req.getRequestURI());
                resp.setHeader("Servlet-Name", getServletName());
                resp.getWriter().write(alias);
            }
        });
        reg.addMapping(alias);

        return reg;
    }

    // ---------------------------------------------------------- Nested Classes

    public static final class RequestListener implements ServletRequestListener {

        static boolean destroyed;
        static boolean initialized;

        @Override
        public void requestDestroyed(ServletRequestEvent servletRequestEvent) {
            destroyed = true;
        }

        @Override
        public void requestInitialized(ServletRequestEvent servletRequestEvent) {
            initialized = true;
        }

    }

}
