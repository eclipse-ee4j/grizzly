/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Basic JMX initialization tests.
 */
public class HttpServerJmxBasicTest {

    @Test
    public void transport() throws Exception {
        HttpServer gws = new HttpServer();
        HttpServer gws1 = new HttpServer();
        NetworkListener listener1 = new NetworkListener("listener1", "localhost", 19080);
        NetworkListener listener2 = new NetworkListener("listener2", "localhost", 19081);
        gws.addListener(listener1);
        gws1.addListener(listener2);

        try {
            gws.start();
            gws1.start();
            gws.getServerConfiguration().setJmxEnabled(true);
            gws1.getServerConfiguration().setJmxEnabled(true);
            assertTrue(true);
        } finally {
            gws.shutdownNow();
            gws1.shutdownNow();
        }
    }
}
