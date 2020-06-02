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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.glassfish.grizzly.servlet.FilterRegistration;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;

/**
 * OSGi Main HttpHandler.
 * <p/>
 * Dispatching HttpHandler. Grizzly integration.
 * <p/>
 * Responsibilities:
 * <ul>
 * <li>Manages registration data.</li>
 * <li>Dispatching {@link HttpHandler#service(Request, Response)} method call to registered {@link HttpHandler}s.</li>
 * </ul>
 *
 * @author Hubert Iwaniuk
 */
public class OSGiMainHandler extends HttpHandler implements OSGiHandler {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Logger logger;
    private final Bundle bundle;
    private final OSGiCleanMapper mapper;

    /**
     * Constructor.
     * 
     * @param logger Logger utility.
     * @param bundle Bundle that we create if for, for local data reference.
     */
    public OSGiMainHandler(Logger logger, Bundle bundle) {
        this.logger = logger;
        this.bundle = bundle;
        this.mapper = new OSGiCleanMapper(logger);
    }

    /**
     * Service method dispatching to registered handlers.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public void service(Request request, Response response) throws Exception {
        boolean invoked = false;
        String alias = request.getDecodedRequestURI();
        String originalAlias = alias;
        logger.debug("Serviceing URI: " + alias);
        // first lookup needs to be done for full match.
        boolean cutOff = false;
        while (true) {
            logger.debug("CutOff: " + cutOff + ", alias: " + alias);
            alias = OSGiCleanMapper.map(alias, cutOff);
            if (alias == null) {
                if (cutOff) {
                    // not found
                    break;
                } else {
                    // switching to reducing mapping mode (removing after last '/' and searching)
                    logger.debug("Swithcing to reducing mapping mode.");
                    cutOff = true;
                    alias = originalAlias;
                }
            } else {
                HttpHandler httpHandler = OSGiCleanMapper.getHttpHandler(alias);

                ((OSGiHandler) httpHandler).getProcessingLock().lock();
                try {
                    updateMappingInfo(request, alias, originalAlias);

                    httpHandler.service(request, response);
                } finally {
                    ((OSGiHandler) httpHandler).getProcessingLock().unlock();
                }
                invoked = true;
                if (response.getStatus() != 404) {
                    break;
                } else if ("/".equals(alias)) {
                    // 404 in "/", cutoff algo will not escape this one.
                    break;
                } else if (!cutOff) {
                    // not found and haven't run in cutoff mode
                    cutOff = true;
                }
            }
        }
        if (!invoked) {
            try {
                response.sendError(404);
            } catch (Exception e) {
                logger.warn("Failed to commit 404 status.", e);
            }
        }
    }

    /**
     * Registers {@link org.glassfish.grizzly.osgi.httpservice.OSGiServletHandler} in OSGi Http Service.
     * <p/>
     * Keeps truck of all registrations, takes care of thread safety.
     *
     * @param alias Alias to register, if wrong value than throws {@link org.osgi.service.http.NamespaceException}.
     * @param servlet Servlet to register under alias, if fails to
     * {@link jakarta.servlet.Servlet#init(jakarta.servlet.ServletConfig)} throws {@link jakarta.servlet.ServletException}.
     * @param initparams Initial parameters to populate {@link jakarta.servlet.ServletContext} with.
     * @param context OSGi {@link org.osgi.service.http.HttpContext}, provides mime handling, security and bundle specific
     * resource access.
     * @param httpService Used to {@link HttpService#createDefaultHttpContext()} if needed.
     * @throws org.osgi.service.http.NamespaceException If alias was invalid or already registered.
     * @throws jakarta.servlet.ServletException If {@link jakarta.servlet.Servlet#init(jakarta.servlet.ServletConfig)}
     * fails.
     */
    public void registerServletHandler(final String alias, final Servlet servlet, final Dictionary initparams, HttpContext context,
            final HttpService httpService) throws NamespaceException, ServletException {

        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {
            validateAlias4RegOk(alias);
            validateServlet4RegOk(servlet);

            if (context == null) {
                logger.debug("No HttpContext provided, creating default");
                context = httpService.createDefaultHttpContext();
            }

            OSGiServletHandler servletHandler = findOrCreateOSGiServletHandler(servlet, context, initparams);
            servletHandler.setServletPath(alias);

            logger.debug("Initializing Servlet been registered");
            servletHandler.startServlet(); // this might throw ServletException, throw it to offending bundle.

            mapper.addHttpHandler(alias, servletHandler);
        } finally {
            lock.unlock();
        }
    }

    /**
     *
     * @param filter
     * @param urlPattern
     * @param initparams
     * @param context
     * @param httpService
     * @throws NamespaceException
     * @throws ServletException
     */
    public void registerFilter(final Filter filter, final String urlPattern, final Dictionary initparams, HttpContext context, final HttpService httpService)
            throws ServletException {

        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {

            if (context == null) {
                logger.debug("No HttpContext provided, creating default");
                context = httpService.createDefaultHttpContext();
            }

            OSGiServletContext servletContext = mapper.getServletContext(context);
            if (servletContext == null) {
                mapper.addContext(context, null);
                servletContext = mapper.getServletContext(context);
            }

            FilterRegistration registration = servletContext.addFilter(Integer.toString(filter.hashCode()), filter);
            registration.addMappingForUrlPatterns(null, urlPattern);

            filter.init(new OSGiFilterConfig(servletContext));

        } finally {
            lock.unlock();
        }
    }

    /**
     * Registers {@link OSGiResourceHandler} in OSGi Http Service.
     * <p/>
     * Keeps truck of all registrations, takes care of thread safety.
     *
     * @param alias Alias to register, if wrong value than throws {@link NamespaceException}.
     * @param context OSGi {@link HttpContext}, provides mime handling, security and bundle specific resource access.
     * @param internalPrefix Prefix to map request for this alias to.
     * @param httpService Used to {@link HttpService#createDefaultHttpContext()} if needed.
     * @throws NamespaceException If alias was invalid or already registered.
     */
    public void registerResourceHandler(String alias, HttpContext context, String internalPrefix, HttpService httpService) throws NamespaceException {

        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {
            validateAlias4RegOk(alias);

            if (context == null) {
                logger.debug("No HttpContext provided, creating default");
                context = httpService.createDefaultHttpContext();
            }
            if (internalPrefix == null) {
                internalPrefix = "";
            }

            OSGiServletContext servletContext = mapper.getServletContext(context);
            if (servletContext == null) {
                mapper.addContext(context, null);
                servletContext = mapper.getServletContext(context);
            }

            mapper.addHttpHandler(alias, new OSGiResourceHandler(alias, internalPrefix, context, servletContext, logger));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unregisters previously registered alias.
     * <p/>
     * Keeps truck of all registrations, takes care of thread safety.
     *
     * @param alias Alias to unregister, if not owning alias {@link IllegalArgumentException} is thrown.
     * @throws IllegalArgumentException If alias was not registered by calling bundle.
     */
    public void unregisterAlias(String alias) {

        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {
            if (mapper.isLocalyRegisteredAlias(alias)) {
                mapper.doUnregister(alias, true);
            } else {
                logger.warn("Bundle: " + bundle + " tried to unregister not owned alias '" + alias + '\'');
                throw new IllegalArgumentException("Alias '" + alias + "' was not registered by you.");
            }
        } finally {
            lock.unlock();
        }
    }

    public void unregisterFilter(final Filter filter) {
        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {
            for (OSGiServletContext servletContext : mapper.httpContextToServletContextMap.values()) {
                servletContext.unregisterFilter(filter);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unregisters all <code>alias</code>es registered by owning bundle.
     */
    public void uregisterAllLocal() {
        logger.info("Unregistering all aliases registered by owning bundle");

        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {
            for (String alias : mapper.getLocalAliases()) {
                logger.debug("Unregistering '" + alias + "'");
                // remember not to call Servlet.destroy() owning bundle might be stopped already.
                mapper.doUnregister(alias, false);
                for (OSGiServletContext servletContext : mapper.httpContextToServletContextMap.values()) {
                    servletContext.unregisterAllFilters();
                }
                mapper.httpContextToServletContextMap.clear();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Part of Shutdown sequence. Unregister and clean up.
     */
    public void unregisterAll() {
        logger.info("Unregistering all registered aliases");

        ReentrantLock lock = OSGiCleanMapper.getLock();
        lock.lock();
        try {
            Set<String> aliases = OSGiCleanMapper.getAllAliases();
            while (!aliases.isEmpty()) {
                String alias = ((TreeSet<String>) aliases).first();
                logger.debug("Unregistering '" + alias + "'");
                // remember not to call Servlet.destroy() owning bundle might be stopped already.
                mapper.doUnregister(alias, false);
            }
        } finally {
            lock.unlock();
        }
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

    /**
     * Chek if <code>alias</code> has been already registered.
     *
     * @param alias Alias to check.
     * @throws NamespaceException If <code>alias</code> has been registered.
     */
    private void validateAlias4RegOk(String alias) throws NamespaceException {
        if (!alias.startsWith("/")) {
            // have to start with "/"
            String msg = "Invalid alias '" + alias + "', have to start with '/'.";
            logger.warn(msg);
            throw new NamespaceException(msg);
        }
        if (alias.length() > 1 && alias.endsWith("/")) {
            // if longer than "/", should not end with "/"
            String msg = "Alias '" + alias + "' can't and with '/' with exception to alias '/'.";
            logger.warn(msg);
            throw new NamespaceException(msg);
        }
        if (OSGiCleanMapper.containsAlias(alias)) {
            String msg = "Alias: '" + alias + "', already registered";
            logger.warn(msg);
            throw new NamespaceException(msg);
        }
    }

    /**
     * Check if <code>servlet</code> has been already registered.
     * <p/>
     * An instance of {@link Servlet} can be registered only once, so in case of servlet been registered before will throw
     * {@link ServletException} as specified in OSGi HttpService Spec.
     *
     * @param servlet {@link Servlet} to check if can be registered.
     * @throws ServletException Iff <code>servlet</code> has been registered before.
     */
    private void validateServlet4RegOk(Servlet servlet) throws ServletException {
        if (OSGiCleanMapper.containsServlet(servlet)) {
            String msg = "Servlet: '" + servlet + "', already registered.";
            logger.warn(msg);
            throw new ServletException(msg);
        }
    }

    /**
     * Looks up {@link OSGiServletHandler}.
     * <p/>
     * If is already registered for <code>httpContext</code> than create new instance based on already registered. Else
     * Create new one.
     * <p/>
     *
     * @param servlet {@link Servlet} been registered.
     * @param httpContext {@link HttpContext} used for registration.
     * @param initparams Init parameters that will be visible in {@link jakarta.servlet.ServletContext}.
     * @return Found or created {@link OSGiServletHandler}.
     */
    private OSGiServletHandler findOrCreateOSGiServletHandler(Servlet servlet, HttpContext httpContext, Dictionary initparams) {
        OSGiServletHandler osgiServletHandler;

        List<OSGiServletHandler> servletHandlers = mapper.getContext(httpContext);
        if (servletHandlers != null) {
            logger.debug("Reusing ServletHandler");
            // new servlet handler for same configuration, different servlet and alias
            osgiServletHandler = servletHandlers.get(0).newServletHandler(servlet);
            servletHandlers.add(osgiServletHandler);
        } else {
            logger.debug("Creating new ServletHandler");
            HashMap<String, String> params;
            if (initparams != null) {
                params = new HashMap<>(initparams.size());
                Enumeration names = initparams.keys();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    params.put(name, (String) initparams.get(name));
                }
            } else {
                params = new HashMap<>(0);
            }

            servletHandlers = new ArrayList<>(1);
            mapper.addContext(httpContext, mapper.getServletContext(httpContext), servletHandlers);

            final OSGiServletContext servletContext = mapper.getServletContext(httpContext);

            assert servletContext != null;

            osgiServletHandler = new OSGiServletHandler(servlet, httpContext, servletContext, params, logger);
            servletHandlers.add(osgiServletHandler);
            osgiServletHandler.setFilterChainFactory(servletContext.getFilterChainFactory());
        }

        return osgiServletHandler;
    }

    private void updateMappingInfo(final Request request, final String alias, final String originalAlias) {

        final MappingData mappingData = request.obtainMappingData();
        mappingData.contextPath.setString("");
        if (alias.equals("/")) {
            mappingData.wrapperPath.setString("");
        } else {
            mappingData.wrapperPath.setString(alias);
        }

        if (alias.length() != originalAlias.length()) {
            String pathInfo = originalAlias.substring(alias.length());
            if (pathInfo.charAt(0) != '/') {
                pathInfo = "/" + pathInfo;
            }

            mappingData.pathInfo.setString(pathInfo);
        }

        updatePaths(request, mappingData);
    }
}
