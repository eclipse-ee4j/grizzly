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

package org.glassfish.grizzly.websockets;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.glassfish.grizzly.http.HttpRequestPacket;

public class EchoServlet extends HttpServlet {
    public static final String RESPONSE_TEXT = "Nothing to see";
    private final WebSocketApplication app;

    public EchoServlet() {
        app = new WebSocketApplication() {
            @Override
            public boolean isApplicationRequest(HttpRequestPacket request) {
                return "/echo".equals(request.getRequestURI());
            }

            public void onMessage(WebSocket socket, String data) {
                socket.send(data);
            }
        };
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        WebSocketEngine.getEngine().register(config.getServletContext().getContextPath(), "/echo", app);
    }

    @Override
    public void destroy() {
        WebSocketEngine.getEngine().unregister(app);
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain; charset=iso-8859-1");
        resp.getWriter().write(RESPONSE_TEXT);
        resp.getWriter().flush();
    }
}
