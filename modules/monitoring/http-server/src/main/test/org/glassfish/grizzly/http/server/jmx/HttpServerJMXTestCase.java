/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.jmx;

import org.glassfish.grizzly.http.server.*;
import org.junit.Test;

import java.io.IOException;


public class HttpServerJMXTestCase {

    @Test
    public void grizzly1835() throws IOException {
        org.glassfish.grizzly.http.server.HttpServer server = createServer();
        server.start();
        server.shutdown();

        org.glassfish.grizzly.http.server.HttpServer server2 = createServer();
        server2.start();
        server2.shutdown();
    }

    private org.glassfish.grizzly.http.server.HttpServer createServer() {
        org.glassfish.grizzly.http.server.HttpServer server2 = new org.glassfish.grizzly.http.server.HttpServer();
        ServerConfiguration serverConfiguration2 = server2.getServerConfiguration();
        serverConfiguration2.setName("fizzbuzz");
        serverConfiguration2.setJmxEnabled(true);
        return server2;
    }
}
