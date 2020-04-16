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

package org.glassfish.grizzly.osgi.httpservice;

import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import java.util.Dictionary;

/**
 * Grizzly OSGi HttpService implementation.
 *
 * @author Hubert Iwaniuk
 * @since Jan 20, 2009
 */
public class HttpServiceImpl implements HttpServiceExtension {

    private final Logger logger;
    private final Bundle bundle;

    final OSGiMainHandler mainHttpHandler;


    // ------------------------------------------------------------ Constructors


    /**
     * {@link HttpService} constructor.
     *
     * @param bundle {@link org.osgi.framework.Bundle} that got this instance of {@link org.osgi.service.http.HttpService}.
     * @param logger {@link org.glassfish.grizzly.osgi.httpservice.util.Logger} utility to be used here.
     */
    public HttpServiceImpl(final Bundle bundle, final Logger logger) {
        this.bundle = bundle;
        this.logger = logger;
        mainHttpHandler = new OSGiMainHandler(logger, bundle);
    }


    // ------------------------------------------------ Methods from HttpService


    /**
     * {@inheritDoc}
     */
    @Override
    public HttpContext createDefaultHttpContext() {
        return new HttpContextImpl(bundle);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerServlet(
            final String alias, final Servlet servlet, final Dictionary initparams, HttpContext httpContext)
            throws ServletException, NamespaceException {

        logger.info("Registering servlet: " + servlet + ", under: "
                            + alias + ", with: " + initparams
                            + " and context: " + httpContext);

        mainHttpHandler.registerServletHandler(alias, servlet, initparams, httpContext, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerResources(final String alias, String prefix, HttpContext httpContext)
            throws NamespaceException {

        logger.info("Registering resource: alias: "
                            + alias + ", prefix: " + prefix
                            + " and context: " + httpContext);

        mainHttpHandler.registerResourceHandler(alias, httpContext, prefix, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(final String alias) {
        logger.info("Unregistering alias: " + alias);
        mainHttpHandler.unregisterAlias(alias);
    }


    // --------------------------------------- Methods from HttpServiceExtension


    /**
     * {@inheritDoc}
     */
    @Override
    public void registerFilter(Filter filter, String urlPattern, Dictionary initParams, HttpContext context)
    throws ServletException {
        logger.info("Registering servlet: "
                            + filter + ", under url-pattern: "
                            + urlPattern + ", with: "
                            + initParams + " and context: " + context);
        mainHttpHandler.registerFilter(filter, urlPattern, initParams, context, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterFilter(Filter filter) {
        logger.info("Unregister filter: " + filter);
        mainHttpHandler.unregisterFilter(filter);
    }

}
