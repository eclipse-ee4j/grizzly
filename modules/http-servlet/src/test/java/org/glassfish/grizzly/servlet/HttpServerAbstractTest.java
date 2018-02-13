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

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.glassfish.grizzly.http.server.HttpServer;

/**
 * Contains utility methods for testing {@link HttpServer}.
 *
 * @author Hubert Iwaniuk
 */
public abstract class HttpServerAbstractTest extends TestCase {
    protected HttpServer httpServer;

    protected String readResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        return reader.readLine();
    }

    protected StringBuilder readMultilineResponse(HttpURLConnection conn) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while((line = reader.readLine())!=null){
            sb.append(line).append("\n");
        }
        return sb;
    }

    protected HttpURLConnection getConnection(String alias, int port) throws IOException {
        HttpURLConnection urlConn = createConnection(alias, port);
        urlConn.connect();
        return urlConn;
    }

    protected HttpURLConnection createConnection(String alias, int port) throws IOException {
        URL url = new URL("http", "localhost", port, alias);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setReadTimeout(10 * 1000);
        return urlConn;
    }

    protected int getResponseCodeFromAlias(HttpURLConnection urlConn)
            throws IOException {
        return urlConn.getResponseCode();
    }

    protected void startHttpServer(int port) throws IOException {
        newHttpServer(port);
        httpServer.start();
    }

    protected void stopHttpServer() {
        httpServer.shutdownNow();
    }

    protected void newHttpServer(int port) throws IOException {
        httpServer = HttpServer.createSimpleServer("./", port);
    }

}
