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

package org.glassfish.grizzly.servlet.async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.EnumSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.HttpServerAbstractTest;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Basic {@link AsyncContext} tests.
 */
public class AsyncContextTest extends HttpServerAbstractTest {
    private static final Logger LOGGER = Grizzly.logger(AsyncContextTest.class);

    public static final int PORT = PORT();

    public void testAsyncContextComplete() throws IOException {
        System.out.println("testAsyncContextComplete");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "foobar", "/servletPath/*", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    if (!req.isAsyncSupported()) {
                        throw new ServletException("Async not supported when it should");
                    }

                    final AsyncContext ac = req.startAsync();

                    Timer asyncTimer = new Timer("AsyncTimer", true);
                    asyncTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                final ServletResponse response = ac.getResponse();
                                response.setContentType("text/plain");
                                final PrintWriter writer = response.getWriter();
                                writer.println("Hello world");
                                ac.complete();
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    }, 1000);
                }
            });
            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/contextPath/servletPath/pathInfo", PORT);
            assertEquals(200, conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            assertEquals("Hello world", reader.readLine());
        } finally {
            stopHttpServer();
        }
    }

    public void testAsyncListenerOnComplete() throws IOException {
        System.out.println("testAsyncListenerOnComplete");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "foobar", "/servletPath/*", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    if (!req.isAsyncSupported()) {
                        throw new ServletException("Async not supported when it should");
                    }

                    AsyncContext ac = req.startAsync(req, resp);
                    ac.addListener(ac.createListener(MyAsyncListener.class));
                    ac.complete();
                }
            });
            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/contextPath/servletPath/pathInfo", PORT);
            assertEquals(200, conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            assertEquals("onComplete", reader.readLine());
        } finally {
            stopHttpServer();
        }
    }

    public void testAsyncListenerOnTimeout() throws IOException {
        System.out.println("testAsyncListenerOnTimeout");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "foobar", "/servletPath/*", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    if (!req.isAsyncSupported()) {
                        throw new ServletException("Async not supported when it should");
                    }

                    AsyncContext ac = req.startAsync(req, resp);
                    ac.setTimeout(1000);
                    ac.addListener(ac.createListener(MyAsyncListener.class));
                }
            });
            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/contextPath/servletPath/pathInfo", PORT);
            assertEquals(200, conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            assertEquals("onTimeout", reader.readLine());
        } finally {
            stopHttpServer();
        }
    }

    public void testAsyncContextHasOriginalRequestAndResponse() throws IOException {
        System.out.println("testAsyncListenerOnTimeout");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "MyServlet", "/test", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    AsyncContext ac = null;
                    String mode = req.getParameter("mode");
                    if ("noarg".equals(mode)) {
                        ac = req.startAsync();
                    } else if ("original".equals(mode)) {
                        ac = req.startAsync(req, res);
                    } else if ("wrap".equals(mode)) {
                        ac = req.startAsync(req, res);
                    } else {
                        throw new ServletException("Invalid mode");
                    }
                }
            });

            addFilter(ctx, "MyFilter", "/test", new Filter() {

                @Override
                public void init(FilterConfig filterConfig) throws ServletException {
                }

                @Override
                public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                    String mode = request.getParameter("mode");
                    if (!"noarg".equals(mode) && !"original".equals(mode) && !"wrap".equals(mode)) {
                        throw new ServletException("Invalid mode");
                    }

                    if ("wrap".equals(mode)) {
                        chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request), response);
                    } else {
                        chain.doFilter(request, response);
                    }

                    AsyncContext ac = request.getAsyncContext();
                    if ("noarg".equals(mode) && !ac.hasOriginalRequestAndResponse()) {
                        throw new ServletException("AsycContext#hasOriginalRequestAndResponse returned false, " + "should have returned true");
                    } else if ("original".equals(mode) && !ac.hasOriginalRequestAndResponse()) {
                        throw new ServletException("AsycContext#hasOriginalRequestAndResponse returned false, " + "should have returned true");
                    } else if ("wrap".equals(mode) && ac.hasOriginalRequestAndResponse()) {
                        throw new ServletException("AsycContext#hasOriginalRequestAndResponse returned true, " + "should have returned false");
                    }

                    ac.complete();
                }

                @Override
                public void destroy() {
                }

            });
            ctx.deploy(httpServer);
            httpServer.start();

            HttpURLConnection conn1 = getConnection("/contextPath/test?mode=noarg", PORT);
            assertEquals(200, conn1.getResponseCode());

            HttpURLConnection conn2 = getConnection("/contextPath/test?mode=original", PORT);
            assertEquals(200, conn2.getResponseCode());

            HttpURLConnection conn3 = getConnection("/contextPath/test?mode=wrap", PORT);
            assertEquals(200, conn3.getResponseCode());
        } finally {
            stopHttpServer();
        }
    }

    private ServletRegistration addServlet(final WebappContext ctx, final String name, final String alias, Servlet servlet) {

        final ServletRegistration reg = ctx.addServlet(name, servlet);
        reg.addMapping(alias);

        return reg;
    }

    private FilterRegistration addFilter(final WebappContext ctx, final String name, final String alias, final Filter filter) {

        final FilterRegistration reg = ctx.addFilter(name, filter);
        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), alias);

        return reg;
    }

    public static class MyAsyncListener implements AsyncListener {

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            event.getAsyncContext().getResponse().getWriter().println("onComplete");
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
            event.getAsyncContext().getResponse().getWriter().println("onTimeout");
            event.getAsyncContext().complete();
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
            event.getAsyncContext().getResponse().getWriter().println("onStartAsync");
        }
    }

}
