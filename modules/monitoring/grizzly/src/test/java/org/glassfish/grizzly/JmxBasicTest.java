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

package org.glassfish.grizzly;

import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Basic JMX initialization tests.
 * 
 * @author Alexey Stashok
 */
public class JmxBasicTest {

    @Test
    public void transport() throws Exception {
        GrizzlyJmxManager manager = GrizzlyJmxManager.instance();
        final TCPNIOTransport transport1 = TCPNIOTransportBuilder.newInstance().build();
        final TCPNIOTransport transport2 = TCPNIOTransportBuilder.newInstance().build();

        try {
            JmxObject jmxTransportObject1 = (JmxObject) transport1
                    .getMonitoringConfig().createManagementObject();
            
            assertNotNull(jmxTransportObject1);
            
            JmxObject jmxTransportObject2 = (JmxObject) transport2
                    .getMonitoringConfig().createManagementObject();

            assertNotNull(jmxTransportObject2);
            
            manager.registerAtRoot(jmxTransportObject1, "Transport1");
            manager.registerAtRoot(jmxTransportObject2, "Transport2");

            transport1.start();

            transport1.bind(7787);

            assertTrue(true);
        } finally {
            transport1.shutdownNow();
            transport2.shutdownNow();
        }
    }
}
