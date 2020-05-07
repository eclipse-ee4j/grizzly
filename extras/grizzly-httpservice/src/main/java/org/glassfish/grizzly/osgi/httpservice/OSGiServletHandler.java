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

import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.glassfish.grizzly.servlet.FilterChainFactory;
import org.glassfish.grizzly.servlet.ServletConfigImpl;
import org.glassfish.grizzly.servlet.WebappContext;
import org.osgi.service.http.HttpContext;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.glassfish.grizzly.servlet.ServletHandler;

/**
 * OSGi customized {@link ServletHandler}.
 *
 * @author Hubert Iwaniuk
 */
public class OSGiServletHandler extends ServletHandler implements OSGiHandler {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private HttpContext httpContext;
    private final Logger logger;
    private String servletPath;

    public OSGiServletHandler(final Servlet servlet,
                              final HttpContext httpContext,
                              final OSGiServletContext servletContext,
                              final HashMap<String, String> servletInitParams,
                              final Logger logger) {
        super(createServletConfig(servletContext, servletInitParams));
        //noinspection AccessingNonPublicFieldOfAnotherObject
        super.servletInstance = servlet;
        this.httpContext = httpContext;
        this.logger = logger;
    }

    private OSGiServletHandler(final ServletConfigImpl servletConfig,
                               final Logger logger) {
        super(servletConfig);
        this.logger = logger;

    }

    public OSGiServletHandler newServletHandler(Servlet servlet) {
        OSGiServletHandler servletHandler =
                new OSGiServletHandler(getServletConfig(), logger);

        servletHandler.setServletInstance(servlet);
        servletHandler.setServletPath(getServletPath());
        servletHandler.setFilterChainFactory(filterChainFactory);
        //noinspection AccessingNonPublicFieldOfAnotherObject
        servletHandler.httpContext = httpContext;
        return servletHandler;
    }

    /**
     * Starts {@link Servlet} instance of this {@link OSGiServletHandler}.
     *
     * @throws ServletException If {@link Servlet} startup failed.
     */
    public void startServlet() throws ServletException {
        configureServletEnv();
        servletInstance.init(getServletConfig());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReentrantReadWriteLock.ReadLock getProcessingLock() {
        return lock.readLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReentrantReadWriteLock.WriteLock getRemovalLock() {
        return lock.writeLock();
    }

    protected void setServletPath(final String path) {
        this.servletPath = path;
    }

    protected String getServletPath() {
        return servletPath;
    }

    public HttpContext getHttpContext() {
        return httpContext;
    }


    // ------------------------------------------------------- Protected Methods


    @Override
    protected void setFilterChainFactory(FilterChainFactory filterChainFactory) {
        super.setFilterChainFactory(filterChainFactory);
    }


    // --------------------------------------------------------- Private Methods



    private static ServletConfigImpl createServletConfig(final OSGiServletContext ctx,
                                                         final Map<String,String> params) {

        final OSGiServletConfig config = new OSGiServletConfig(ctx);
        config.setInitParameters(params);
        return config;
    }


    // ---------------------------------------------------------- Nested Classes


    private static final class OSGiServletConfig extends ServletConfigImpl {

        protected OSGiServletConfig(WebappContext servletContextImpl) {
            super(servletContextImpl);
        }

        @Override
        protected void setInitParameters(Map<String, String> parameters) {
            super.setInitParameters(parameters);
        }
    }

}
