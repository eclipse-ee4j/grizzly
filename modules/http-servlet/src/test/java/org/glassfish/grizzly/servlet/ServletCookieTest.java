/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static junit.framework.Assert.assertEquals;
import org.glassfish.grizzly.http.HttpPacket;

/**
 * Verify that request processing isn't broken by malformed Cookies
 * 
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class ServletCookieTest extends HttpServerAbstractTest {

    private static final int PORT = 12345;
    private static final String CONTEXT = "/test";
    private static final String SERVLETMAPPING = "/servlet";
    private static final String FIRST_COOKIE_NAME = "firstCookie";
    private static final String FIRST_COOKIE_VALUE = "its_a_me-firstCookie";
    private static final String SECOND_COOKIE_NAME = "secondCookie";
    private static final String SECOND_COOKIE_VALUE = "{\"a\": 1,\"Version\":2}";
    private static final String THIRD_COOKIE_NAME = "thirdCookie";
    private static final String THIRD_COOKIE_VALUE = "its_a_me-thirdCookie";
    
    /**
     * Assert basic cookie parsing 
     * @throws Exception 
     */
    public void testServletCookieParsing() throws Exception {
        
        try {
            startHttpServer(PORT);

            WebappContext ctx = new WebappContext("Test", CONTEXT);
            ServletRegistration servletRegistration = ctx.addServlet("intervalServlet", new HttpServlet() {

                @Override
                protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                    Cookie[] cookies = req.getCookies();
                    assertEquals(3, cookies.length);
                    Cookie currentCookie = cookies[0];
                    assertEquals(FIRST_COOKIE_NAME, currentCookie.getName());
                    assertEquals(FIRST_COOKIE_VALUE, currentCookie.getValue());
                    
                    currentCookie = cookies[1];
                    assertEquals(SECOND_COOKIE_NAME, currentCookie.getName());
                    /* The cookie isn't read completely but instead of throwing
                     * an Exception we discard the remainder and continue request
                     * processing while logging an error.
                     */
                    assertEquals("{\"a\": 1", currentCookie.getValue());
                    
                    currentCookie = cookies[2];
                    assertEquals(THIRD_COOKIE_NAME, currentCookie.getName());
                    assertEquals(THIRD_COOKIE_VALUE, currentCookie.getValue());
                }
            });

            servletRegistration.addMapping(SERVLETMAPPING);
            ctx.deploy(httpServer);

            //build and send request
            StringBuilder sb = new StringBuilder(256);
            sb.append(FIRST_COOKIE_NAME).append("=").append(FIRST_COOKIE_VALUE);
            sb.append(";");
            sb.append(SECOND_COOKIE_NAME).append("=").append(SECOND_COOKIE_VALUE);
            sb.append(";");
            sb.append(THIRD_COOKIE_NAME).append("=").append(THIRD_COOKIE_VALUE);
                    
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Cookie", sb.toString());
            HttpPacket request = ClientUtil.createRequest(CONTEXT + SERVLETMAPPING, PORT, headers);
            ClientUtil.sendRequest(request, 60, PORT);
        } finally {
            stopHttpServer();
        }
    }

}
