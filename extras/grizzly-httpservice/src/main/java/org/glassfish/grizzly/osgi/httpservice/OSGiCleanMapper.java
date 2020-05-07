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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.osgi.service.http.HttpContext;

import jakarta.servlet.Servlet;
import java.util.concurrent.locks.ReentrantLock;
import org.glassfish.grizzly.http.server.HttpHandler;

/**
 * Context mapper.
 * Supports complex context.
 *
 * @author Hubert Iwaniuk
 */
class OSGiCleanMapper {

    private static final ReentrantLock lock = new ReentrantLock();
    private static final TreeSet<String> aliasTree = new TreeSet<String>();
    private static final Map<String, HttpHandler> registrations = new HashMap<String, HttpHandler>(16);
    private static final Set<Servlet> registeredServlets = new HashSet<Servlet>(16);

    private final Set<String> localAliases = new HashSet<String>(4);
    private final HashMap<HttpContext, List<OSGiServletHandler>> contextServletHandlerMap =
            new HashMap<HttpContext, List<OSGiServletHandler>>(3);
    private final Logger logger;

    protected final Map<HttpContext, OSGiServletContext> httpContextToServletContextMap =
                new HashMap<HttpContext, OSGiServletContext>();


    // ------------------------------------------------------------ Constructors


    protected OSGiCleanMapper(final Logger logger) {
        this.logger = logger;
    }


    // ---------------------------------------------------------- Public Methods


    /**
     * Performs mapping of requested URI to registered alias if any.
     * <p/>
     * Works in two modes:
     * <ul>
     * <li>Full match - Checks for full match of resource (cutAfterSlash == false),</li>
     * <li>Reducing match - Checks {@link String#substring(int, int)} (0, {@link String#lastIndexOf(String)} ('/'))
     * for match (cutAfterSlash == true).</li>
     * </ul>
     *
     * @param resource      Resource to be mapped.
     * @param cutAfterSlash Should cut off after last '/' before looking up.
     * @return First matching alias, or <code>null</code> if no match has been found.
     */
    public static String map(String resource, boolean cutAfterSlash) {
        String result;
        String match = resource;
        while (true) {
            int i = 0;
            if (cutAfterSlash) {
                i = match.lastIndexOf('/');
                if (i == -1) {
                    result = null;
                    break;
                } else {
                    if (i == 0)
                        match = "/";
                    else
                        match = resource.substring(0, i);
                }
            }
            if (containsAlias(match)) {
                result = match;
                break;
            } else if (i == 0) {
                result = null;
                break;
            }
        }
        return result;
    }

    /**
     * Checks if alias has been registered.
     *
     * @param alias Alias to check.
     * @return <code>true</code> if alias has been registered, else <code>false</code>.
     */
    public static boolean containsAlias(String alias) {
        return aliasTree.contains(alias);
    }

    /**
     * Checks if {@link Servlet} has been registered.
     *
     * @param servlet Servlet instance to check.
     * @return <code>true</code> if alias has been registered, else <code>false</code>.
     */
    public static boolean containsServlet(Servlet servlet) {
        return registeredServlets.contains(servlet);
    }

    /**
     * Gets mappers {@link ReentrantLock}.
     * <p/>
     * This {@link java.util.concurrent.locks.Lock} should protect mappers state.
     *
     * @return {@link java.util.concurrent.locks.Lock} to protect operations on mapper.
     */
    public static ReentrantLock getLock() {
        return lock;
    }

    /**
     * Looks up {@link HttpHandler} registered under alias.
     *
     * @param alias Registered alias.
     * @return {@link HttpHandler} registered under alias.
     */
    static HttpHandler getHttpHandler(String alias) {
        return registrations.get(alias);
    }

    /**
     * Remove registration information for internal book keeping.
     *
     * @param alias Alias to unregister.
     */
    public void recycleRegistrationData(String alias) {
        if (containsAlias(alias)) {
            // global cleanup
            aliasTree.remove(alias);
            HttpHandler handler = registrations.remove(alias);
            handler.destroy();

            // local cleanup
            localAliases.remove(alias);
        }
    }

    /**
     * Add {@link HttpHandler}.
     * <p/>
     *
     * @param alias   Registration alias.
     * @param handler HttpHandler handling requests for <code>alias</code>.
     */
    public void addHttpHandler(String alias, HttpHandler handler) {
        if (!containsAlias(alias)) {
            registerAliasHandler(alias, handler);
            if (handler instanceof OSGiServletHandler) {
                registeredServlets.add(((OSGiServletHandler) handler).getServletInstance());
            }
            localAliases.add(alias);
        }
    }

    /**
     * Checks if alias was registered by calling bundle.
     *
     * @param alias Alias to check for local registration.
     * @return <code>true</code> if alias was registered locally, else <code>false</code>.
     */
    public boolean isLocalyRegisteredAlias(String alias) {
        return localAliases.contains(alias);
    }

    /**
     * Executes unregistering of <code>alias</code> optionally calling {@link jakarta.servlet.Servlet#destroy()}.
     *
     * @param alias                Alias to unregister.
     * @param callDestroyOnServlet If <code>true</code> call {@link jakarta.servlet.Servlet#destroy()}, else don't call.
     */
    public void doUnregister(String alias, boolean callDestroyOnServlet) {
        if (containsAlias(alias)) {
            HttpHandler httpHandler = getHttpHandler(alias);
            if (httpHandler instanceof OSGiServletHandler) {
                ((OSGiHandler) httpHandler).getRemovalLock().lock();
                try {
                    Servlet servlet = ((OSGiServletHandler) httpHandler).getServletInstance();
                    registeredServlets.remove(servlet);
                    if (callDestroyOnServlet) {
                        servlet.destroy();
                    }
                } finally {
                    ((OSGiHandler) httpHandler).getRemovalLock().unlock();
                }
            }
        }
        recycleRegistrationData(alias);
    }

    /**
     * Gets locally registered aliases.
     *
     * @return Unmodifiable {@link Set} of locally registered aliases.
     */
    public Set<String> getLocalAliases() {
        return Collections.unmodifiableSet(localAliases);
    }

    /**
     * Gets all registered aliases.
     *
     * @return {@link Set} of all registered aliases.
     */
    /*package*/ static Set<String> getAllAliases() {
        return aliasTree;
    }
    /**
     * Checks if {@link HttpContext} has been registered..
     *
     * @param httpContext Context to check.
     * @return <code>true</code> if httpContext has been registered.
     */
    public boolean containsContext(HttpContext httpContext) {
        return contextServletHandlerMap.containsKey(httpContext);
    }

    public List<OSGiServletHandler> getContext(HttpContext httpContext) {
        return contextServletHandlerMap.get(httpContext);
    }

    public void addContext(final HttpContext httpContext,
            final List<OSGiServletHandler> servletHandlers) {
        addContext(httpContext, null, servletHandlers);
    }

    public void addContext(final HttpContext httpContext,
            OSGiServletContext servletCtx,
            final List<OSGiServletHandler> servletHandlers) {
        if (servletCtx == null) {
            servletCtx = new OSGiServletContext(httpContext, logger);
        }
        
        contextServletHandlerMap.put(httpContext, servletHandlers);
        httpContextToServletContextMap.put(httpContext, servletCtx);
    }
    
    public OSGiServletContext getServletContext(final HttpContext httpContext) {
        return httpContextToServletContextMap.get(httpContext);
    }

    private static boolean registerAliasHandler(String alias, HttpHandler httpHandler) {
        boolean wasNew = aliasTree.add(alias);
        if (wasNew) {
            registrations.put(alias, httpHandler);
        }
        return wasNew;
    }
}
