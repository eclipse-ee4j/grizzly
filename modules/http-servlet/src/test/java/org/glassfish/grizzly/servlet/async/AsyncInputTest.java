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
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.HttpServerAbstractTest;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ReadListener;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Basic Servlet 3.1 non-blocking input tests.
 */
public class AsyncInputTest extends HttpServerAbstractTest {
    private static final Logger LOGGER = Grizzly.logger(AsyncInputTest.class);

    public static final int PORT = 18890 + 17;

    public void testNonBlockingInput() throws IOException {
        System.out.println("testNonBlockingInput");
        try {
            newHttpServer(PORT);
            WebappContext ctx = new WebappContext("Test", "/contextPath");
            addServlet(ctx, "foobar", "/servletPath/*", new HttpServlet() {

                @Override
                protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
                    ServletOutputStream output = res.getOutputStream();
                    ServletInputStream input = req.getInputStream();

                    final AsyncContext asyncCtx = req.startAsync();

                    final byte[] buffer = new byte[1024];

                    ReadListener readListener = new ReadListenerImpl(asyncCtx, buffer);
                    input.setReadListener(readListener);

                    int len;
                    while (input.isReady() && (len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                }
            });

            ctx.deploy(httpServer);
            httpServer.start();

            HttpURLConnection conn = createConnection("/contextPath/servletPath/pathInfo", PORT);
            conn.setChunkedStreamingMode(5);
            conn.setDoOutput(true);
            conn.connect();

            BufferedReader input = null;
            BufferedWriter output = null;
            boolean expected;
            try {
                output = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
                try {
                    String data = "Hello";
                    output.write(data);
                    output.flush();
                    int sleepInSeconds = 3;
                    System.out.println("Sleeping " + sleepInSeconds + " seconds");
                    Thread.sleep(sleepInSeconds * 1000);
                    data = "World";
                    output.write(data);
                    output.flush();
                    output.close();
                } catch (Exception ex) {
                }
                input = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String line;
                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                    expected = line.endsWith("-onAllDataRead");
                    if (expected) {
                        break;
                    }
                }
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                } catch (Exception ex) {
                }

                try {
                    if (output != null) {
                        output.close();
                    }
                } catch (Exception ex) {
                }
            }
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

    private static class ReadListenerImpl implements ReadListener {
        private final AsyncContext asyncCtx;
        private final byte[] buffer;

        private ReadListenerImpl(AsyncContext asyncCtx, byte[] buffer) {
            this.asyncCtx = asyncCtx;
            this.buffer = buffer;
        }

        @Override
        public void onDataAvailable() {
            try {
                ServletInputStream input = asyncCtx.getRequest().getInputStream();
                ServletOutputStream output = asyncCtx.getResponse().getOutputStream();

                output.print("onDataAvailable-");
                int len;
                while (input.isReady() && (len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            } catch (Throwable t) {
                onError(t);
            }
        }

        @Override
        public void onAllDataRead() {
            try {
                ServletInputStream input = asyncCtx.getRequest().getInputStream();
                ServletOutputStream output = asyncCtx.getResponse().getOutputStream();

                output.println("-onAllDataRead");

                asyncCtx.complete();
            } catch (Throwable t) {
                onError(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            LOGGER.log(Level.WARNING, "Unexpected error", t);
            asyncCtx.complete();
        }
    }
}
