/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.osgi.httpservice;

import org.glassfish.grizzly.http.util.MimeType;
import org.glassfish.grizzly.servlet.FilterChainFactory;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.osgi.service.http.HttpContext;
import org.glassfish.grizzly.osgi.httpservice.util.Logger;

import jakarta.servlet.Filter;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.IOException;
import java.util.EventListener;

import static java.text.MessageFormat.format;

/**
 * OSGi {@link WebappContext} integration.
 *
 * @author Hubert Iwaniuk
 */
public class OSGiServletContext extends WebappContext {
    /**
     * {@link HttpContext} providing OSGi integration.
     */
    private final HttpContext httpContext;
    private final Logger logger;


    // ------------------------------------------------------------ Constructors


    /**
     * Default constructor.
     *
     * @param httpContext {@link org.osgi.service.http.HttpContext} to provide integration with OSGi.
     * @param logger      Logger util.
     */
    public OSGiServletContext(HttpContext httpContext, Logger logger) {
        this.httpContext = httpContext;
        this.logger = logger;
        installAuthFilter(httpContext);
    }


    // ---------------------------------------------------------- Public Methods


    /**
     * OSGi integration. Uses {@link HttpContext#getResource(String)}.
     * <p/>
     * {@inheritDoc}
     */
    @Override public URL getResource(String path) throws MalformedURLException {
        if (path == null || !path.startsWith("/")) {
            throw new MalformedURLException(path);
        }

        path = normalize(path);
        if (path == null)
            return (null);

        return httpContext.getResource(path);
    }

    /**
     * OSGi integration. Uses {@link HttpContext#getResource(String)}.
     * <p/>
     * {@inheritDoc}
     */
    @Override public InputStream getResourceAsStream(String path) {
        path = normalize(path);
        if (path == null)
            return (null);

        URL resource = httpContext.getResource(path);
        if (resource == null) {
            logger.warn(format("Error getting resource ''{0}''. Message: {1}", path, "Can't locate resource."));
            return null;
        }

        try {
            return resource.openStream();
        } catch (IOException e) {
            logger.warn(format("Error getting resource ''{0}''. Message: {1}", path, e.getMessage()));
        }
        return null;
    }

    /**
     * OSGi integration. Uses {@link HttpContext#getMimeType(String)}.
     * <p/>
     * {@inheritDoc}
     */
    @Override public String getMimeType(String file) {
        String mime = httpContext.getMimeType(file);
        if (mime == null) {
            // if returned null, try figuring out by ourselfs.
            mime = MimeType.getByFilename(file);
        }
        return mime;
    }


    // ------------------------------------------------------- Protected Methods


    @Override
    protected EventListener[] getEventListeners() {
        return super.getEventListeners();
    }

    @Override
    protected FilterChainFactory getFilterChainFactory() {
        return super.getFilterChainFactory();
    }

    @Override
    protected void unregisterFilter(final Filter f) {
        super.unregisterFilter(f);
    }

    @Override
    protected void unregisterAllFilters() {
        super.unregisterAllFilters();
    }


    // --------------------------------------------------------- Private Methods


    private void installAuthFilter(HttpContext httpContext) {
        final Filter f = new OSGiAuthFilter(httpContext);
        try {
            f.init(new OSGiFilterConfig(this));
        } catch (Exception ignored) {
            // won't happen
        }
        FilterRegistration registration =
                addFilter(Integer.toString(f.hashCode()), f);
        registration.addMappingForUrlPatterns(null, "/*");
    }
}
