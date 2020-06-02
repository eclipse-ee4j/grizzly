/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.httpmultipart;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * Simple Grizzly {@link HttpHandler}, which returns HTML upload form.
 *
 * @author Alexey Stashok
 */
public class FormHttpHandler extends HttpHandler {

    @Override
    public void service(final Request request, final Response response) throws Exception {
        // Set the response content type
        response.setContentType("text/html");

        // Return the HTML upload form
        response.getWriter()
                .write("<form action=\"upload\" method=\"post\" enctype=\"multipart/form-data\">" + "Description: <input name=\"description\"/><br/>"
                        + "Select File: <input type=\"file\" name=\"fileName\"/><br/>" + "<input type=\"submit\" value=\"Submit\"/></form>");
    }
}
