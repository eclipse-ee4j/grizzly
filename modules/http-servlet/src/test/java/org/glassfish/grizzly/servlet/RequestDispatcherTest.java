/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;

/**
 * Request/NamedDispatcher Test
 *
 * @author Bongjae Chang
 */
public class RequestDispatcherTest extends HttpServerAbstractTest {

    private static final int PORT = 18890 + 12;

    public void testForward() throws IOException {
        System.out.println("testForward");
        try {
            newHttpServer(PORT);

            String contextPath = "/webapp";
            WebappContext ctx = new WebappContext("Test", contextPath);
            ServletRegistration servlet1 = ctx.addServlet("servlet1", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet1");

                    // relative path test
                    RequestDispatcher dispatcher = request.getRequestDispatcher("servlet2");
                    assertNotNull(dispatcher);
                    dispatcher.forward(request, response);
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");
            ServletRegistration servlet2 = ctx.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet2");
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");
            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/webapp/servlet1", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("Hello, world! I am a servlet2", readMultilineResponse(conn).toString().trim());
        } finally {
            stopHttpServer();
        }
    }

    public void testForwardRootContext() throws IOException {
        System.out.println("testForwardRootContext");
        try {
            newHttpServer(PORT);

            String contextPath = "/";
            WebappContext ctx = new WebappContext("Test", contextPath);
            ServletRegistration servlet1 = ctx.addServlet("servlet1", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet1");

                    // relative path test
                    RequestDispatcher dispatcher = request.getRequestDispatcher("servlet2");
                    assertNotNull(dispatcher);
                    dispatcher.forward(request, response);
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");
            ServletRegistration servlet2 = ctx.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet2");
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");
            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/servlet1", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("Hello, world! I am a servlet2", readMultilineResponse(conn).toString().trim());
        } finally {
            stopHttpServer();
        }
    }

    public void testInclude() throws IOException {
        System.out.println("testInclude");
        try {
            newHttpServer(PORT);

            String contextPath = "/webapp";
            WebappContext ctx = new WebappContext("Test", contextPath);
            ServletRegistration servlet1 = ctx.addServlet("servlet1", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet1");

                    // relative path test
                    RequestDispatcher dispatcher = request.getRequestDispatcher("servlet2");
                    assertNotNull(dispatcher);
                    dispatcher.include(request, response);
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");
            ServletRegistration servlet2 = ctx.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet2");
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");
            ctx.deploy(httpServer);

            httpServer.start();
            HttpURLConnection conn = getConnection("/webapp/servlet1", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("Hello, world! I am a servlet1\nHello, world! I am a servlet2", readMultilineResponse(conn).toString().trim());
        } finally {
            stopHttpServer();
        }
    }

    public void testIncludeRootContext() throws IOException {
        System.out.println("testIncludeRootContext");
        try {
            newHttpServer(PORT);

            String contextPath = "/";
            WebappContext ctx = new WebappContext("Test", contextPath);
            ServletRegistration servlet1 = ctx.addServlet("servlet1", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet1");

                    // relative path test
                    RequestDispatcher dispatcher = request.getRequestDispatcher("servlet2");
                    assertNotNull(dispatcher);
                    dispatcher.include(request, response);
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");
            ServletRegistration servlet2 = ctx.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet2");
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");
            ctx.deploy(httpServer);

            httpServer.start();
            HttpURLConnection conn = getConnection("/servlet1", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("Hello, world! I am a servlet1\nHello, world! I am a servlet2", readMultilineResponse(conn).toString().trim());
        } finally {
            stopHttpServer();
        }
    }

    public void testNamedDispatcherForward() throws IOException {
        System.out.println("testNamedDispatcherForward");
        try {
            newHttpServer(PORT);

            String contextPath = "/webapp";
            WebappContext ctx = new WebappContext("Test", contextPath);
            ServletRegistration servlet1 = ctx.addServlet("servlet1", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet1");

                    ServletContext servletCtx = getServletContext();
                    assertNotNull(servletCtx);
                    RequestDispatcher dispatcher = servletCtx.getNamedDispatcher("servlet2");
                    assertNotNull(dispatcher);
                    dispatcher.forward(request, response);
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");

            ServletRegistration servlet2 = ctx.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet2");
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");

            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/webapp/servlet1", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("Hello, world! I am a servlet2", readMultilineResponse(conn).toString().trim());
        } finally {
            stopHttpServer();
        }
    }

    public void testNamedDispatcherInclude() throws IOException {
        System.out.println("testNamedDispatcherInclude");
        try {
            newHttpServer(PORT);

            String contextPath = "/webapp";
            WebappContext ctx = new WebappContext("Test", contextPath);
            ServletRegistration servlet1 = ctx.addServlet("servlet1", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet1");

                    ServletContext servletCtx = getServletContext();
                    assertNotNull(servletCtx);
                    RequestDispatcher dispatcher = servletCtx.getNamedDispatcher("servlet2");
                    assertNotNull(dispatcher);
                    dispatcher.include(request, response);
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");

            ServletRegistration servlet2 = ctx.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println("Hello, world! I am a servlet2");
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");

            ctx.deploy(httpServer);
            httpServer.start();
            HttpURLConnection conn = getConnection("/webapp/servlet1", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("Hello, world! I am a servlet1\nHello, world! I am a servlet2", readMultilineResponse(conn).toString().trim());
        } finally {
            stopHttpServer();
        }
    }

    public void testCrossContextForward() throws IOException {
        System.out.println("testCrossContextForward");
        try {
            newHttpServer( PORT );
            WebappContext ctx1 = new WebappContext("ctx1", "/webapp1");
            ServletRegistration servlet1 = ctx1.addServlet("servlet1",new HttpServlet() {
                @Override
                public void doGet( HttpServletRequest request, HttpServletResponse response )
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println( "Hello, world! I am a servlet1" );

                    ServletContext servletCtx1 = getServletContext();
                    assertNotNull( servletCtx1 );
                    RequestDispatcher dispatcher = servletCtx1.getNamedDispatcher("servlet2");
                    assertNull( dispatcher );

                    // cross context
                    ServletContext servletCtx2 = servletCtx1.getContext( "/webapp2" );
                    assertNotNull( servletCtx2 );
                    // The pathname must begin with a "/"
                    dispatcher = servletCtx2.getRequestDispatcher( "/servlet2" );
                    assertNotNull( dispatcher );
                    dispatcher.forward( request, response );
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");

            WebappContext ctx2 = new WebappContext("ctx2", "/webapp2");
            ServletRegistration servlet2 = ctx2.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet( HttpServletRequest request, HttpServletResponse response )
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println( "Hello, world! I am a servlet2" );
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");

            ctx1.deploy(httpServer);
            ctx2.deploy(httpServer);

            httpServer.start();
            HttpURLConnection conn = getConnection( "/webapp1/servlet1", PORT );
            assertEquals( HttpServletResponse.SC_OK, getResponseCodeFromAlias( conn ) );
            assertEquals( "Hello, world! I am a servlet2", readMultilineResponse( conn ).toString().trim() );
        } finally {
            stopHttpServer();
        }
    }

    public void testComplexDispatch() throws IOException {
        // servlet1 --> dispatcher forward by ServletRequest's API(servlet2) ->
        // named dispatcher include(servlet3) -> cross context, dispatcher include by ServletContext's API(servlet4)
        System.out.println("testComplexDispatch");
        try {
            newHttpServer( PORT );
            WebappContext ctx1 = new WebappContext("ctx1", "/webapp1");
            // webapp1(servlet1, servlet2, servlet3)
            ServletRegistration servlet1 = ctx1.addServlet("servlet1",new HttpServlet() {
                @Override
                public void doGet( HttpServletRequest request, HttpServletResponse response )
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println( "Hello, world! I am a servlet1" );

                    RequestDispatcher dispatcher = request.getRequestDispatcher( "servlet2" );
                    assertNotNull( dispatcher );
                    dispatcher.forward( request, response );
                    out.close();
                }
            });
            servlet1.addMapping("/servlet1");

            ServletRegistration servlet2 = ctx1.addServlet("servlet2", new HttpServlet() {
                @Override
                public void doGet( HttpServletRequest request, HttpServletResponse response )
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println( "Hello, world! I am a servlet2" );

                    ServletContext servletCtx = getServletContext();
                    assertNotNull( servletCtx );
                    RequestDispatcher dispatcher = servletCtx.getNamedDispatcher( "servlet3" );
                    assertNotNull( dispatcher );
                    dispatcher.include( request, response );
                    out.close();
                }
            });
            servlet2.addMapping("/servlet2");

            ServletRegistration servlet3 = ctx1.addServlet("servlet3",new HttpServlet() {
                @Override
                public void doGet( HttpServletRequest request, HttpServletResponse response )
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println( "Hello, world! I am a servlet3" );

                    ServletContext servletCtx1 = getServletContext();
                    assertNotNull( servletCtx1 );
                    ServletContext servletCtx2 = servletCtx1.getContext( "/webapp2" );
                    assertNotNull( servletCtx2 );
                    RequestDispatcher dispatcher = servletCtx2.getRequestDispatcher( "/servlet4" );
                    dispatcher.include( request, response );
                    out.close();
                }
            });
            servlet3.addMapping("/servlet3");

            // webapp2(servlet4)
            WebappContext ctx2 = new WebappContext("ctx2", "/webapp2");
            ServletRegistration servlet4 = ctx2.addServlet("servlet4",new HttpServlet() {
                @Override
                public void doGet( HttpServletRequest request, HttpServletResponse response )
                        throws ServletException, IOException {
                    PrintWriter out = response.getWriter();
                    out.println( "Hello, world! I am a servlet4" );
                    out.close();
                }
            });
            servlet4.addMapping("/servlet4");

            ctx1.deploy(httpServer);
            ctx2.deploy(httpServer);

            httpServer.start();
            HttpURLConnection conn = getConnection( "/webapp1/servlet1", PORT );
            assertEquals( HttpServletResponse.SC_OK, getResponseCodeFromAlias( conn ) );
            assertEquals( "Hello, world! I am a servlet2\nHello, world! I am a servlet3\nHello, world! I am a servlet4",
                          readMultilineResponse( conn ).toString().trim() );
        } finally {
            stopHttpServer();
        }
    }
}
