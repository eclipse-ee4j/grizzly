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
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.servlet.HttpServerAbstractTest;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Basic {@link AsyncContext} tests.
 */
public class AsyncDispatchTest extends HttpServerAbstractTest {
    private static final Logger LOGGER = Grizzly.logger(AsyncDispatchTest.class);

    public static final int PORT = PORT();

    public void testAsyncContextDispatchZeroArg() throws Exception {
        System.out.println("testAsyncContextDispatchZeroArg");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "foobar", "/servletPath/*", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    if (!req.isAsyncSupported()) {
                        throw new ServletException("Async not supported when it should");
                    }
                    if (!"MYVALUE".equals(req.getAttribute("MYNAME"))) {
                        final AsyncContext ac = req.startAsync();
                        req.setAttribute("MYNAME", "MYVALUE");

                        Timer asyncTimer = new Timer("AsyncTimer", true);
                        asyncTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ac.dispatch();
                            }
                        }, 1000);
                    } else {
                        // Async re-dispatched request
                        res.getWriter().println("Hello world");
                    }
                }
            });
            ctx.deploy(httpServer);
            
            Thread.sleep(10);
            httpServer.start();
            HttpURLConnection conn = getConnection("/contextPath/servletPath/pathInfo", PORT);
            assertEquals(200, conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            assertEquals("Hello world", reader.readLine());
        } finally {
            stopHttpServer();
        }
    }

    public void testAsyncContextDispatchToPath() throws IOException {
        System.out.println("testAsyncContextDispatchToPath");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "TestServlet", "/TestServlet", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    if (!req.isAsyncSupported()) {
                        throw new ServletException("Async not supported when it should");
                    }

                    final AsyncContext ac = req.startAsync();
                    req.setAttribute("MYNAME", "MYVALUE");

                    final String target = req.getParameter("target");

                    Timer asyncTimer = new Timer("AsyncTimer", true);
                    asyncTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            ac.dispatch(target);
                        }
                    }, 1000);
                }
            });
            addServlet(ctx, "DispatchTarget", "/DispatchTargetWithPath", new HttpServlet() {
                private static final String EXPECTED_ASYNC_REQUEST_URI = "/contextPath/TestServlet";

                private static final String EXPECTED_ASYNC_SERVLET_PATH = "/TestServlet";

                private static final String EXPECTED_ASYNC_QUERY_STRING = "target=DispatchTargetWithPath";

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    Enumeration<String> attrNames = req.getAttributeNames();
                    if (attrNames == null) {
                        throw new ServletException("Missing ASYNC dispatch related " + "request attributes");
                    }

                    if (!"MYVALUE".equals(req.getAttribute("MYNAME"))) {
                        throw new ServletException("Missing custom request attribute");
                    }

                    int asyncRequestAttributeFound = 0;
                    while (attrNames.hasMoreElements()) {
                        String attrName = attrNames.nextElement();
                        if (AsyncContext.ASYNC_REQUEST_URI.equals(attrName)) {
                            if (!EXPECTED_ASYNC_REQUEST_URI.equals(req.getAttribute(attrName))) {
                                throw new ServletException("Wrong value for " + AsyncContext.ASYNC_REQUEST_URI + " request attribute. Found: "
                                        + req.getAttribute(attrName) + ", expected: " + EXPECTED_ASYNC_REQUEST_URI);
                            }
                            asyncRequestAttributeFound++;
                        } else if (AsyncContext.ASYNC_CONTEXT_PATH.equals(attrName)) {
                            if (!getServletContext().getContextPath().equals(req.getAttribute(attrName))) {
                                throw new ServletException("Wrong value for " + AsyncContext.ASYNC_CONTEXT_PATH + " request attribute. Found: "
                                        + req.getAttribute(attrName) + ", expected: " + getServletContext().getContextPath());
                            }
                            asyncRequestAttributeFound++;
                        } else if (AsyncContext.ASYNC_PATH_INFO.equals(attrName)) {
                            if (req.getAttribute(attrName) != null) {
                                throw new ServletException("Wrong value for " + AsyncContext.ASYNC_PATH_INFO + " request attribute");
                            }
                            asyncRequestAttributeFound++;
                        } else if (AsyncContext.ASYNC_SERVLET_PATH.equals(attrName)) {
                            if (!EXPECTED_ASYNC_SERVLET_PATH.equals(req.getAttribute(attrName))) {
                                throw new ServletException("Wrong value for " + AsyncContext.ASYNC_SERVLET_PATH + " request attribute. Found "
                                        + req.getAttribute(attrName) + ", expected: " + EXPECTED_ASYNC_SERVLET_PATH);
                            }
                            asyncRequestAttributeFound++;
                        } else if (AsyncContext.ASYNC_QUERY_STRING.equals(attrName)) {
                            if (!EXPECTED_ASYNC_QUERY_STRING.equals(req.getAttribute(attrName))) {
                                throw new ServletException("Wrong value for " + AsyncContext.ASYNC_QUERY_STRING + " request attribute. Found: "
                                        + req.getAttribute(attrName) + ", expected: " + EXPECTED_ASYNC_QUERY_STRING);
                            }
                            asyncRequestAttributeFound++;
                        }
                    }

                    if (asyncRequestAttributeFound != 5) {
                        throw new ServletException("Wrong number of ASYNC dispatch " + "related request attributes");
                    }

                    res.getWriter().println("Hello world");
                }
            });

            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/contextPath/TestServlet?target=DispatchTargetWithPath", PORT);
            assertEquals(200, conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            assertEquals("Hello world", reader.readLine());
        } finally {
            stopHttpServer();
        }
    }

    public void testAsyncContextMultipleDispatch() throws IOException {
        System.out.println("testAsyncContextMultipleDispatch");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");

            addServlet(ctx, "foobar", "/servletPath/*", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    if (!req.isAsyncSupported()) {
                        throw new ServletException("Async not supported when it should");
                    }

                    if (req.getDispatcherType() == DispatcherType.REQUEST) {
                        // Container-initiated dispatch
                        req.setAttribute("ABC", "DEF");
                        final AsyncContext ac = req.startAsync();
                        ac.addListener(new AsyncListener() {
                            @Override
                            public void onComplete(AsyncEvent event) throws IOException {
                                event.getAsyncContext().getResponse().getWriter().println("onComplete");
                            }

                            @Override
                            public void onTimeout(AsyncEvent event) throws IOException {
                                // do nothing
                            }

                            @Override
                            public void onError(AsyncEvent event) throws IOException {
                                // do nothing
                            }

                            @Override
                            public void onStartAsync(AsyncEvent event) throws IOException {
                                event.getAsyncContext().getResponse().getWriter().print("onStartAsync,");
                                /*
                                 * ServletRequest#startAsync clears the list of AsyncListener instances registered with the AsyncContext - after calling
                                 * each AsyncListener at its onStartAsync method, which is the method we're in. Register ourselves again, so we continue
                                 * to get notified
                                 */
                                event.getAsyncContext().addListener(this);
                            }
                        });
                        Timer asyncTimer = new Timer("AsyncTimer", true);
                        asyncTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ac.dispatch();
                            }
                        }, 1000);
                    } else if (req.getDispatcherType() == DispatcherType.ASYNC) {
                        if ("DEF".equals(req.getAttribute("ABC"))) {
                            // First async dispatch
                            req.removeAttribute("ABC");
                            req.startAsync().dispatch();
                        } else {
                            // Second async dispatch
                            req.startAsync().complete();
                        }
                    }
                }
            });
            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/contextPath/servletPath/pathInfo", PORT);
            assertEquals(200, conn.getResponseCode());
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            assertEquals("onStartAsync,onStartAsync,onComplete", reader.readLine());
        } finally {
            stopHttpServer();
        }
    }

    public void testDispatchForwardAsyncDispatch() throws IOException {
        System.out.println("testDispatchForwardAsyncDispatch");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");

            addServlet(ctx, "test.AsyncDispatch", "/asyncdispatch", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    System.out.println("AD: dispatcher type: " + req.getDispatcherType());
                    boolean withArgs = Boolean.parseBoolean(req.getParameter("withargs"));
                    boolean forceAsync = Boolean.parseBoolean(req.getParameter("forceasync"));
                    if (!req.getDispatcherType().equals(DispatcherType.ASYNC) || forceAsync) {

                        final AsyncContext ac = withArgs ? req.startAsync(req, res) : req.startAsync();

                        ac.addListener(new AsyncListener() {
                            @Override
                            public void onComplete(AsyncEvent event) {
                                System.out.println("AD: AsyncListener.onComplete");
                            }

                            @Override
                            public void onError(AsyncEvent event) {
                                System.out.println("AD: AsyncListener.onError");
                            }

                            @Override
                            public void onStartAsync(AsyncEvent event) {
                                System.out.println("AD: AsyncListener.onStartAsync");
                            }

                            @Override
                            public void onTimeout(AsyncEvent event) {
                                System.out.println("AD: AsyncListener.onTimeout");
                            }
                        });

                        Timer timer = new Timer("AsyncTimer", true);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                ac.dispatch();
                            }
                        }, 3000);
                    } else {
                        PrintWriter writer = res.getWriter();
                        writer.write("Hello from AsyncDispatch\n");
                    }
                }
            });

            addServlet(ctx, "test.DispatchForward", "/dispatchforward", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    String forwardUrl = "/asyncdispatch";
                    String withargs = req.getParameter("withargs");
                    if (withargs != null) {
                        forwardUrl = forwardUrl + "?withargs=" + withargs;
                    }

                    if (!req.getDispatcherType().equals(DispatcherType.ASYNC)) {
                        System.out.println("DF: forwarding " + forwardUrl);
                        req.getRequestDispatcher(forwardUrl).forward(req, res);
                    } else {
                        System.out.println("DF: async dispatch type ...");
                        PrintWriter writer = res.getWriter();
                        writer.write("Hello from DispatchForward\n");
                    }
                }
            });

            addServlet(ctx, "test.DispatchForward0", "/dispatchforward0", new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    String forwardUrl = "/dispatchforward";

                    if (!req.getDispatcherType().equals(DispatcherType.ASYNC)) {
                        System.out.println("DF0: forwarding " + forwardUrl);
                        req.getRequestDispatcher(forwardUrl).forward(req, res);
                    } else {
                        System.out.println("DF0: async dispatch type ...");
                        PrintWriter writer = res.getWriter();
                        writer.write("Hello from DispatchForward\n");
                    }
                }
            });

            addServlet(ctx, "test.NamedDispatchForward0", "/nameddispatchforward0", new HttpServlet() {
                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    String servletName = "test.DispatchForward";
                    if (!req.getDispatcherType().equals(DispatcherType.ASYNC)) {
                        System.out.println("DF0: named forwarding " + servletName);
                        getServletContext().getNamedDispatcher(servletName).forward(req, res);
                    } else {
                        System.out.println("DF0: async dispatch type ...");
                        PrintWriter writer = res.getWriter();
                        writer.write("Hello from DispatchForward0\n");
                    }

                }
            });

            ctx.deploy(httpServer);
            httpServer.start();

            assertEquals("Hello from DispatchForward", doTest("/contextPath/dispatchforward"));
            assertEquals("Hello from AsyncDispatch", doTest("/contextPath/dispatchforward?withargs=true"));

            // double dispatch
            assertEquals("Hello from DispatchForward", doTest("/contextPath/dispatchforward0"));
            // named dispatch
            assertEquals("Hello from DispatchForward0", doTest("/contextPath/nameddispatchforward0"));
        } finally {
            stopHttpServer();
        }
    }

    private String doTest(String uri) throws IOException {
        HttpURLConnection conn = getConnection(uri, PORT);
        assertEquals(200, conn.getResponseCode());

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        return reader.readLine();
    }

    private ServletRegistration addServlet(final WebappContext ctx, final String name, final String alias, Servlet servlet) {

        final ServletRegistration reg = ctx.addServlet(name, servlet);
        reg.addMapping(alias);

        return reg;
    }
}
