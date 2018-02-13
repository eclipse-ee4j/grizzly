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

package filter;

import org.glassfish.grizzly.servlet.FilterRegistration;
import com.sun.jersey.api.core.ClasspathResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.servlet.WebappContext;

public class Main {

    public static final URI BASE_URI = UriBuilder.fromUri("http://localhost/").port(9998).build();

    protected static HttpServer startServer() throws IOException {
        final Map<String, String> initParams = new HashMap<String, String>();

        initParams.put("com.sun.jersey.config.property.packages",
                "filter");

//        System.out.println("Starting grizzly...");
        return create(BASE_URI, initParams);
    }

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = startServer();
//        System.out.println(String.format("Jersey app started with WADL available at " + "%sapplication.wadl\nHit enter to stop it...",
//                BASE_URI));
        System.in.read();
        httpServer.shutdownNow();
    }

    private static HttpServer create(URI u,
            Map<String, String> initParams) throws IOException {
        return create(u, ServletContainer.class, initParams);
    }

    private static HttpServer create(URI u, Class<? extends Filter> c,
            Map<String, String> initParams) throws IOException {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }
        WebappContext ctx = new WebappContext("Test", "/");
        FilterRegistration registration = ctx.addFilter("TestFilter", c);
        if (initParams == null) {
            registration.setInitParameter(ClasspathResourceConfig.PROPERTY_CLASSPATH,
                    System.getProperty("java.class.path").replace(File.pathSeparatorChar, ';'));
        } else {
            for (Map.Entry<String, String> e : initParams.entrySet()) {
                registration.setInitParameter(e.getKey(), e.getValue());
            }
        }
        registration.addMappingForUrlPatterns(null, "/*");

        String path = u.getPath();
        if (path == null) {
            throw new IllegalArgumentException("The URI path, of the URI " + u +
                    ", must be non-null");
        } else if (path.length() == 0) {
            throw new IllegalArgumentException("The URI path, of the URI " + u +
                    ", must be present");
        } else if (path.charAt(0) != '/') {
            throw new IllegalArgumentException("The URI path, of the URI " + u +
                    ". must start with a '/'");
        }

        return create(u, ctx, path);
    }

    private static Filter getInstance(Class<? extends Filter> c) {
        try {
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpServer create(URI u, WebappContext ctx, String path)
            throws IOException, IllegalArgumentException {
        if (u == null) {
            throw new IllegalArgumentException("The URI must not be null");
        }

        // TODO support https
        final String scheme = u.getScheme();
        if (!scheme.equalsIgnoreCase("http")) {
            throw new IllegalArgumentException("The URI scheme, of the URI " + u +
                    ", must be equal (ignoring case) to 'http'");
        }

        final int port = (u.getPort() == -1) ? 80 : u.getPort();

        final HttpServer server = HttpServer.createSimpleServer("./tmp", port);

        ctx.deploy(server);

        server.start();

        return server;
    }
}
