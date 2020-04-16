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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import junit.framework.TestCase;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.Processor;
import org.glassfish.grizzly.http.server.HttpServer;

/**
 * {@link HttpServer} tests.
 *
 * @author Sebastien Dionne
 * @since 2009/04/15
 */
public class HelloHttpServerTest extends TestCase {

    public static final int PORT = 18890 + 11;
    private static final Logger logger = Grizzly.logger(HelloHttpServerTest.class);
    private HttpServer httpServer;

    public void testNPERegression() throws IOException {
        System.out.println("testNPERegression");
        try {
            createHttpServer(PORT);
            String[] aliases = new String[] { "*.php" };
            WebappContext ctx = new WebappContext("Test");
            ServletRegistration servlet = ctx.addServlet("TestServet", HelloServlet.class);
            servlet.addMapping(aliases);
            ctx.deploy(httpServer);
            httpServer.start();

            String context = "/";
            String servletPath = "war_autodeploy/php_test";
            String url = context + servletPath + "/index.php";
            HttpURLConnection conn = getConnection(url);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));

            String response = readResponse(conn).toString();
            assertEquals("Hello, world!", response.trim());

        } finally {
            stopHttpServer();
        }
    }

    public void testMultiPath() throws IOException {
        System.out.println("testMultiPath");
        try {
            createHttpServer(PORT);
            String[] aliases = new String[] { "*.php" };
            WebappContext ctx = new WebappContext("Test");

            ServletRegistration servlet =
                    ctx.addServlet("TestServlet", HelloServlet.class.getName());
            servlet.setLoadOnStartup(1);
            servlet.addMapping(aliases);
            ctx.deploy(httpServer);
            httpServer.start();

            String context = "/";
            String servletPath = "notvalid/php_test";
            String url = context + servletPath + "/index.php";
            HttpURLConnection conn = getConnection(url);
            assertEquals(HttpServletResponse.SC_OK, getResponseCodeFromAlias(conn));

            String response = readResponse(conn).toString();
            assertEquals("Hello, world!", response.trim());

            // should failed
            url = context + servletPath + "/hello.1";
            conn = getConnection(url);
            assertEquals(HttpServletResponse.SC_NOT_FOUND, getResponseCodeFromAlias(conn));


        } finally {
            stopHttpServer();
        }
    }

    public void testProtocolFilter() throws IOException {
        System.out.println("testProtocolFilter");
        try {
            String[] aliases = new String[] { "*.foo" };
            WebappContext ctx = new WebappContext("Test");
            ServletRegistration servlet =
                    ctx.addServlet("TestServlet", HelloServlet.class);
            servlet.addMapping(aliases);
            httpServer = HttpServer.createSimpleServer(".", PORT);
            httpServer.start();
            ctx.deploy(httpServer);

            Processor pc = httpServer.getListener("grizzly").getTransport().getProcessor();
            System.out.println("ProtcolChain: " + pc);
            assertNotNull(pc);
        } finally {
            stopHttpServer();
        }
    }


    // --------------------------------------------------------- Private Methods


     private StringBuffer readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

        StringBuffer sb = new StringBuffer();
        String line;

        while((line = reader.readLine())!=null){
            logger.log(Level.INFO, "received line {0}", line);
            sb.append(line).append("\n");
        }

        return sb;
    }

    private HttpURLConnection getConnection(String path) throws IOException {
        logger.log(Level.INFO, "sending request to {0}", path);
        URL url = new URL("http", "localhost", PORT, path);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.connect();
        return urlConn;
    }

    private int getResponseCodeFromAlias(HttpURLConnection urlConn) throws IOException {
        return urlConn.getResponseCode();
    }


    private void createHttpServer(int port) {
        httpServer = HttpServer.createSimpleServer(".", port);

    }

    private void stopHttpServer() {
        httpServer.shutdownNow();
    }


    // ---------------------------------------------------------- Nested Classes

    /**
     * Hello world servlet.  Most servlets will extend
     * jakarta.servlet.http.HttpServlet as this one does.
     */
    public static class HelloServlet extends HttpServlet {
      /**
       * Implements the HTTP GET method.  The GET method is the standard
       * browser method.
       *
       * @param request the request object, containing data from the browser
       * @param response the response object to send data to the browser
       */
        @Override
      public void doGet (HttpServletRequest request,
                         HttpServletResponse response)
        throws ServletException, IOException
      {

        // Returns a writer to write to the browser
        PrintWriter out = response.getWriter();

        // Writes the string to the browser.
        out.println("Hello, world!");
        out.close();
      }
    }
}
