/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.HttpHandlerChain;

/**
 * Test {@link HttpHandlerChain} use of the {@link MapperTest}
 *
 * @author Jeanfrancois Arcand
 */
public class MapperTest extends HttpServerAbstractTest {

    public static final int PORT = 18080;
    private static final Logger LOGGER = Grizzly.logger(MapperTest.class);

    public void testOverlappingMapping() throws IOException {
        System.out.println("testOverlappingMapping");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String[] aliases = new String[]{"/aaa/bbb", "/aaa/ccc"};
            final String[] mappings = {
                    "Mapping{matchValue='aaa/bbb', pattern='/aaa/bbb', servletName='', mappingMatch=EXACT}",
                    "Mapping{matchValue='aaa/ccc', pattern='/aaa/ccc', servletName='', mappingMatch=EXACT}"
            };
            for (String alias : aliases) {
                addServlet(ctx, alias);
            }
            ctx.deploy(httpServer);
            for (int i = 0, len = aliases.length; i < len; i++) {
                final String alias = aliases[i];
                HttpURLConnection conn = getConnection(alias, PORT);
                assertEquals(HttpServletResponse.SC_OK,
                        getResponseCodeFromAlias(conn));
                assertEquals(alias, readResponse(conn));
                assertEquals(alias, conn.getHeaderField("servlet-path"));
                assertNull(alias, conn.getHeaderField("path-info"));
                assertEquals(mappings[i], conn.getHeaderField("http-servlet-mapping"));
            }
        } finally {
            stopHttpServer();
        }
   }


    public void testOverlappingMapping2() throws IOException {
        System.out.println("testOverlappingMapping2");
        try {
            startHttpServer(PORT);

            String[] alias = new String[]{"*.jsp", "/jsp/*"};

            WebappContext ctx = new WebappContext("Test");
            addServlet(ctx, "*.jsp");
            addServlet(ctx, "/jsp/*");
            ctx.deploy(httpServer);

            HttpURLConnection conn = getConnection("/jsp/index.jsp", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals(alias[1], readResponse(conn));
            assertEquals("/jsp", conn.getHeaderField("servlet-path"));
            assertEquals("/index.jsp", conn.getHeaderField("path-info"));
            assertEquals("Mapping{matchValue='index.jsp', pattern='/jsp/*', servletName='', mappingMatch=PATH}", conn.getHeaderField("http-servlet-mapping"));
        } finally {
            stopHttpServer();
        }
   }


    public void testRootStarMapping() throws IOException {
        System.out.println("testRootMapping");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "/*";
            addServlet(ctx, alias);   // overrides the static resource handler
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/index.html", PORT);
            assertEquals(HttpServletResponse.SC_OK,
                    getResponseCodeFromAlias(conn));
            assertEquals(alias, readResponse(conn));
            assertEquals("", conn.getHeaderField("servlet-path"));
            assertEquals("/index.html", conn.getHeaderField("path-info"));
            assertEquals("Mapping{matchValue='index.html', pattern='/*', servletName='', mappingMatch=PATH}", conn.getHeaderField("http-servlet-mapping"));
        } finally {
            stopHttpServer();
        }
    }

    public void testRootStarMapping2() throws IOException {
            System.out.println("testRootMapping");
            try {
                startHttpServer(PORT);
                WebappContext ctx = new WebappContext("Test");
                String alias = "/*";
                addServlet(ctx, alias);   // overrides the static resource handler
                ctx.deploy(httpServer);
                HttpURLConnection conn = getConnection("/foo/index.html", PORT);
                assertEquals(HttpServletResponse.SC_OK,
                        getResponseCodeFromAlias(conn));
                assertEquals(alias, readResponse(conn));
                assertEquals("", conn.getHeaderField("servlet-path"));
                assertEquals("/foo/index.html", conn.getHeaderField("path-info"));
                assertEquals("Mapping{matchValue='foo/index.html', pattern='/*', servletName='', mappingMatch=PATH}", conn.getHeaderField("http-servlet-mapping"));
            } finally {
                stopHttpServer();
            }
        }

    public void testRootMapping() throws IOException {
        System.out.println("testRootMapping");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "/";
            addServlet(ctx, alias);   // overrides the static resource handler
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/", PORT);
            assertEquals(HttpServletResponse.SC_OK,
                    getResponseCodeFromAlias(conn));
            assertEquals(alias, readResponse(conn));
            assertEquals("/", conn.getHeaderField("servlet-path"));
            assertEquals(null, conn.getHeaderField("path-info"));
            assertEquals("Mapping{matchValue='', pattern='/', servletName='', mappingMatch=DEFAULT}", conn.getHeaderField("http-servlet-mapping"));
        } finally {
            stopHttpServer();
        }
    }

    public void testRootMapping2() throws IOException {
        System.out.println("testRootMapping");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "/";
            addServlet(ctx, alias);   // overrides the static resource handler
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/foo/index.html", PORT);
            assertEquals(HttpServletResponse.SC_OK,
                    getResponseCodeFromAlias(conn));
            assertEquals(alias, readResponse(conn));
            assertEquals("/foo/index.html", conn.getHeaderField("servlet-path"));
            assertEquals(null, conn.getHeaderField("path-info"));
        assertEquals("Mapping{matchValue='foo/index.html', pattern='/', servletName='', mappingMatch=DEFAULT}", conn.getHeaderField("http-servlet-mapping"));} finally {
            stopHttpServer();
        }
    }

    public void testWrongMapping() throws IOException {
        System.out.println("testWrongMapping");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "/a/b/c";
            addServlet(ctx, alias);
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/aaa.html", PORT);
            assertEquals(HttpServletResponse.SC_NOT_FOUND,
                    getResponseCodeFromAlias(conn));
        } finally {
            stopHttpServer();
        }
    }
//
//    public void testComplexMapping() throws IOException {
//        System.out.println("testComplexMapping");
//        try {
//            startHttpServer(PORT);
//            String alias = "/a/b/c/*.html";
//            addHttpHandler(alias);
//            HttpURLConnection conn = getConnection("/a/b/c/index.html", PORT);
//            assertEquals(HttpServletResponse.SC_OK,
//                    getResponseCodeFromAlias(conn));
//            assertEquals(alias, readResponse(conn));
//        } finally {
//            stopHttpServer();
//        }
//    }
//
    public void testWildcardMapping() throws IOException {
        System.out.println("testWildcardMapping");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "*.html";
            addServlet(ctx, alias);
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/index.html", PORT);
            assertEquals(HttpServletResponse.SC_OK,
                    getResponseCodeFromAlias(conn));
            assertEquals(alias, readResponse(conn));
            assertEquals("/index.html", conn.getHeaderField("servlet-path"));
            assertNull(conn.getHeaderField("path-info"));
            assertEquals("Mapping{matchValue='index', pattern='*.html', servletName='', mappingMatch=EXTENSION}", conn.getHeaderField("http-servlet-mapping"));
        } finally {
            stopHttpServer();
        }
    }

     public void testWrongMappingRootContext() throws IOException {
        System.out.println("testWrongMappingRootContext");
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "*.a";
            addServlet(ctx, alias);
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/aaa.html", PORT);
            assertEquals(HttpServletResponse.SC_NOT_FOUND, getResponseCodeFromAlias(conn));
        } finally {
            stopHttpServer();
        }
    }


    public void testDefaultServletOverride() throws Exception {
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test");
            String alias = "/";
            addServlet(ctx, alias);
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("/", conn.getHeaderField("servlet-path"));
            assertEquals("Mapping{matchValue='', pattern='/', servletName='', mappingMatch=DEFAULT}", conn.getHeaderField("http-servlet-mapping"));
        } finally {
            stopHttpServer();
        }
    }

    public void testDefaultContext() throws Exception {
        try {
            startHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/");
            String alias = "/foo/*";
            addServlet(ctx, alias);
            ctx.deploy(httpServer);
            HttpURLConnection conn = getConnection("/foo/bar/baz", PORT);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));
            assertEquals("/foo/bar/baz", conn.getHeaderField("request-uri"));
            assertEquals("", conn.getHeaderField("context-path"));
            assertEquals("/foo", conn.getHeaderField("servlet-path"));
            assertEquals("/bar/baz", conn.getHeaderField("path-info"));
            assertEquals("Mapping{matchValue='bar/baz', pattern='/foo/*', servletName='', mappingMatch=PATH}", conn.getHeaderField("http-servlet-mapping"));
        } finally {
            stopHttpServer();
        }
    }


    // --------------------------------------------------------- Private Methods


    private static void addServlet(final WebappContext ctx, final String alias) {
        final ServletRegistration reg = ctx.addServlet(alias, new HttpServlet() {

            @Override
            protected void doGet(
                    HttpServletRequest req, HttpServletResponse resp)
                    throws IOException {
                LOGGER.log(Level.INFO, "{0} received request {1}", new Object[]{alias, req.getRequestURI()});
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setHeader("Path-Info", req.getPathInfo());
                resp.setHeader("Servlet-Path", req.getServletPath());
                resp.setHeader("Request-Was", req.getRequestURI());
                resp.setHeader("Servlet-Name", getServletName());
                resp.setHeader("Request-Uri", req.getRequestURI());
                resp.setHeader("Context-Path", req.getContextPath());
                resp.setHeader("Http-Servlet-Mapping", req.getHttpServletMapping().toString());
                resp.getWriter().write(alias);
            }
        });
        reg.addMapping(alias);
    }

}
