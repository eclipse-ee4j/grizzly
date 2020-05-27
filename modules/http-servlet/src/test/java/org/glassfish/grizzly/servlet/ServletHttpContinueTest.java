/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(Parameterized.class)
public class ServletHttpContinueTest {

    public static final int PORT = 18890 + 14;

    private final int numberOfExtraHttpHandlers;

    public ServletHttpContinueTest(final int numberOfExtraHttpHandlers) {
        this.numberOfExtraHttpHandlers = numberOfExtraHttpHandlers;
    }

    @Parameters
    public static Collection<Object[]> getNumberOfExtraHttpHandlers() {
        return Arrays.asList(new Object[][] { { 0 }, { 5 } });
    }

    // ------------------------------------------------------------ Test Methods

    @Test
    public void test100Continue() throws Exception {

        final SafeFutureImpl<String> future = new SafeFutureImpl<>();
        HttpServer server = createServer(new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                future.result(request.getParameter("a"));
            }
        }, null, "/path");

        Socket s = null;
        try {
            server.start();
            s = SocketFactory.getDefault().createSocket("localhost", PORT);
            s.setSoTimeout(10 * 1000);

            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            out.write("POST /path HTTP/1.1\r\n".getBytes());
            out.write(("Host: localhost:" + PORT + "\r\n").getBytes());
            out.write("Content-Type: application/x-www-form-urlencoded\r\n".getBytes());
            out.write("Content-Length: 7\r\n".getBytes());
            out.write("Expect: 100-continue\r\n".getBytes());
            out.write("\r\n".getBytes());

            StringBuilder sb = new StringBuilder();
            for (;;) {
                int i = in.read();
                if (i == '\r') {
                    in.mark(6);
                    if (in.read() == '\n' && in.read() == '\r' && in.read() == '\n') {
                        break;
                    } else {
                        in.reset();
                    }
                } else {
                    sb.append((char) i);
                }
            }

            assertEquals("HTTP/1.1 100 Continue", sb.toString().trim());

            // send post data now that we have clearance
            out.write("a=hello\r\n\r\n".getBytes());
            assertEquals("hello", future.get(10, TimeUnit.SECONDS));
            sb.setLength(0);
            for (;;) {
                int i = in.read();
                if (i == '\r') {
                    break;
                } else {
                    sb.append((char) i);
                }
            }

            assertEquals("HTTP/1.1 200 OK", sb.toString().trim());
        } finally {
            server.shutdownNow();
            if (s != null) {
                s.close();
            }
        }

    }

    @Test
    public void testExpectationIgnored() throws Exception {

        HttpServer server = createServer(new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                response.setStatus(404);
            }
        }, null, "/path");

        Socket s = null;
        try {
            server.start();
            s = SocketFactory.getDefault().createSocket("localhost", PORT);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();
            StringBuilder post = new StringBuilder();
            post.append("POST /path HTTP/1.1\r\n");
            post.append("Host: localhost:").append(PORT).append("\r\n");
            post.append("Expect: 100-continue\r\n");
            post.append("Content-Type: application/x-www-form-urlencoded\r\n");
            post.append("Content-Length: 7\r\n");
            post.append("\r\n");
            post.append("a=hello\r\n\r\n");

            out.write(post.toString().getBytes());

            StringBuilder sb = new StringBuilder();
            for (;;) {
                int i = in.read();
                if (i == '\r') {
                    break;
                } else {
                    sb.append((char) i);
                }
            }

            assertEquals("HTTP/1.1 404 Not Found", sb.toString().trim());

        } finally {
            server.shutdownNow();
            if (s != null) {
                s.close();
            }
        }

    }

    @Test
    public void testFailedExpectation() throws Exception {

        HttpServer server = createServer(new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            }
        }, null, "/path");

        Socket s = null;
        try {
            server.start();
            s = SocketFactory.getDefault().createSocket("localhost", PORT);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            out.write("POST /path HTTP/1.1\r\n".getBytes());
            out.write(("Host: localhost:" + PORT + "\r\n").getBytes());
            out.write("Content-Type: application/x-www-form-urlencoded\r\n".getBytes());
            out.write("Content-Length: 7\r\n".getBytes());
            out.write("Expect: 100-Continue-Extension\r\n".getBytes());
            out.write("\r\n".getBytes());

            StringBuilder sb = new StringBuilder();
            for (;;) {
                int i = in.read();
                if (i == '\r') {
                    break;
                } else {
                    sb.append((char) i);
                }
            }

            assertEquals("HTTP/1.1 417 Expectation Failed", sb.toString().trim());

        } finally {
            server.shutdownNow();
            if (s != null) {
                s.close();
            }
        }

    }

    @Test
    public void testCustomFailedExpectation() throws Exception {

        HttpServer server = createServer(new HttpServlet() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            }
        }, new ExpectationHandler() {

            @Override
            public void onExpectAcknowledgement(HttpServletRequest request, HttpServletResponse response, AckAction action) throws Exception {
                action.fail();
            }
        }, "/path");

        Socket s = null;
        try {
            server.start();
            s = SocketFactory.getDefault().createSocket("localhost", PORT);
            OutputStream out = s.getOutputStream();
            InputStream in = s.getInputStream();

            out.write("POST /path HTTP/1.1\r\n".getBytes());
            out.write(("Host: localhost:" + PORT + "\r\n").getBytes());
            out.write("Content-Type: application/x-www-form-urlencoded\r\n".getBytes());
            out.write("Content-Length: 7\r\n".getBytes());
            out.write("Expect: 100-Continue\r\n".getBytes());
            out.write("\r\n".getBytes());

            StringBuilder sb = new StringBuilder();
            for (;;) {
                int i = in.read();
                if (i == '\r') {
                    break;
                } else {
                    sb.append((char) i);
                }
            }

            assertEquals("HTTP/1.1 417 Expectation Failed", sb.toString().trim());

        } finally {
            server.shutdownNow();
            if (s != null) {
                s.close();
            }
        }

    }
    // --------------------------------------------------------- Private Methods

    private HttpServer createServer(final HttpServlet httpServlet, final ExpectationHandler expectationHandler, final String mapping) {

        HttpServer server = new HttpServer();
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, PORT);
        server.addListener(listener);

        for (int i = 0; i < numberOfExtraHttpHandlers; i++) {
            server.getServerConfiguration().addHttpHandler(new StaticHttpHandler(), String.valueOf("/" + i));
        }

        WebappContext ctx = new WebappContext("Test");

        final ServletRegistration reg = ctx.addServlet("TestSerlvet", httpServlet);
        reg.setExpectationHandler(expectationHandler);
        reg.addMapping(mapping);

        ctx.deploy(server);

        return server;

    }

}
