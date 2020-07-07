/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.junit.Test;

/**
 * Test the servlet's parameter "load-on-startup"
 *
 * @author Sebastien Dionne
 *
 */
public class LoadOnStartupTest {

    @Test
    public void loadOnStartupTest() throws Exception {

        WebappContext ctx = new WebappContext("Test");
        ServletRegistration s = ctx.addServlet("t1", "foo.TestServlet");
        assertEquals(-1, s.loadOnStartup);

        s = ctx.addServlet("t2", "foo.TestServlet");
        s.setLoadOnStartup(-5);
        assertEquals(-1, s.loadOnStartup);

        s = ctx.addServlet("t3", "foo.TestServlet");
        s.setLoadOnStartup(0);
        assertEquals(0, s.loadOnStartup);

        s = ctx.addServlet("t4", "foo.TestServlet");
        s.setLoadOnStartup(3);
        assertEquals(3, s.loadOnStartup);

        s = ctx.addServlet("t5", "foo.TestServlet");
        s.setLoadOnStartup(5);
        assertEquals(5, s.loadOnStartup);

        s = ctx.addServlet("t6", "foo.TestServlet");
        s.setLoadOnStartup(2);
        assertEquals(2, s.loadOnStartup);

        java.lang.String[] expectedOrder = { "t3", "t6", "t4", "t5" };

        Collection<? extends ServletRegistration> registrations = ctx.getServletRegistrations().values();

        assertTrue(!registrations.isEmpty());

        LinkedList<ServletRegistration> list = new LinkedList<>(registrations);
        Collections.sort(list);
        int i = 0;
        for (ServletRegistration r : list) {
            if (r.loadOnStartup < 0) {
                continue;
            }
            assertEquals(expectedOrder[i], r.name);
            i++;
        }

    }
}
