/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.sni.httpserver;

import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.ssl.SSLUtils;

/**
 * Simple {@link HttpHandler} implementation.
 */
public class SimpleHttpHandler extends HttpHandler{

    @Override
    public void service(final Request request, final Response response)
            throws Exception {
        response.setContentType("text/plain");
        
        response.getWriter().write("Works fine. Server certificate (DN): " +
                getServerCertificate(request).getIssuerDN().getName());
    }

    private X509Certificate getServerCertificate(final Request request) {
        // return the active server certificate, which is used for this connection.
        final SSLEngine sslEngine = SSLUtils.getSSLEngine(
                request.getContext().getConnection());
        return (X509Certificate) sslEngine.getSession().getLocalCertificates()[0];
    }
}
