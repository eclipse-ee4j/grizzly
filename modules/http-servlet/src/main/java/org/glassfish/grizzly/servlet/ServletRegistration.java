/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletSecurityElement;
import org.glassfish.grizzly.utils.ArraySet;

/**
 * Allows customization of a {@link jakarta.servlet.Servlet} registered with the {@link WebappContext}.
 *
 * @since 2.2
 */
public class ServletRegistration extends Registration
        implements jakarta.servlet.ServletRegistration.Dynamic, Comparable<ServletRegistration> {

    protected Class<? extends Servlet> servletClass;
    protected final ArraySet<String> urlPatterns = new ArraySet<String>(String.class);
    protected Servlet servlet;
    protected int loadOnStartup = -1;
    protected ExpectationHandler expectationHandler;
    protected boolean isAsyncSupported;

    /**
     * The run-as identity for this servlet.
     */
    private String runAs = null;

    // ------------------------------------------------------------ Constructors


    /**
     * Creates a new ServletRegistration associated with the specified
     * {@link WebappContext}.
     *
     * @param ctx the owning {@link WebappContext}.
     * @param name the name of the Filter.
     * @param servletClassName the fully qualified class name of the {@link Servlet}
     *  implementation.
     */
    protected ServletRegistration(final WebappContext ctx,
                                  final String name,
                                  final String servletClassName) {

        super(ctx, name, servletClassName);
        this.name = name;

    }

    /**
     * Creates a new ServletRegistration associated with the specified
     * {@link WebappContext}.
     *
     * @param ctx the owning {@link WebappContext}.
     * @param name the name of the Filter.
     * @param servlet the {@link Servlet} instance.
     */
    protected ServletRegistration(final WebappContext ctx,
                                  final String name,
                                  final Servlet servlet) {

        this(ctx, name, servlet.getClass());
        this.servlet = servlet;

    }

    /**
     * Creates a new ServletRegistration associated with the specified
     * {@link WebappContext}.
     *
     * @param ctx the owning {@link WebappContext}.
     * @param name the name of the Filter.
     * @param servletClass the class of the {@link Servlet} implementation.
     */
    protected ServletRegistration(final WebappContext ctx,
                                  final String name,
                                  final Class<? extends Servlet> servletClass) {

        this(ctx, name, servletClass.getName());
        this.servletClass = servletClass;

    }


    // ---------------------------------------------------------- Public Methods


    /**
     * Adds a servlet mapping with the given URL patterns for the Servlet
     * represented by this ServletRegistration.
     *
     * <p>If any of the specified URL patterns are already mapped to a
     * different Servlet, no updates will be performed.
     *
     * <p>If this method is called multiple times, each successive call
     * adds to the effects of the former.
     *
     * @param urlPatterns the URL patterns of the servlet mapping
     *
     * @return the (possibly empty) Set of URL patterns that are already
     * mapped to a different Servlet
     *
     * @throws IllegalArgumentException if <tt>urlPatterns</tt> is null
     * or empty
     * @throws IllegalStateException if the ServletContext from which this
     * ServletRegistration was obtained has already been initialized
     */
    @Override
    public Set<String> addMapping(String... urlPatterns) {
        if (ctx.deployed) {
            throw new IllegalStateException("WebappContext has already been deployed");
        }
        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException("'urlPatterns' cannot be null or zero-length");
        }
        this.urlPatterns.addAll(urlPatterns);
        return Collections.emptySet(); // TODO - need to comply with the spec at some point
    }

    /**
     * Gets the currently available mappings of the
     * Servlet represented by this <code>ServletRegistration</code>.
     *
     * <p>If permitted, any changes to the returned <code>Collection</code> must not
     * affect this <code>ServletRegistration</code>.
     *
     * @return a (possibly empty) <code>Collection</code> of the currently
     * available mappings of the Servlet represented by this
     * <code>ServletRegistration</code>
     */
    @Override
    public Collection<String> getMappings() {
        return Collections.unmodifiableList(
                Arrays.asList(urlPatterns.getArrayCopy()));
    }

    /**
     * Sets the <code>loadOnStartup</code> priority on the Servlet
     * represented by this dynamic ServletRegistration.
     * <p/>
     * <p>A <tt>loadOnStartup</tt> value of greater than or equal to
     * zero indicates to the container the initialization priority of
     * the Servlet. In this case, the container must instantiate and
     * initialize the Servlet during the initialization phase of the
     * WebappContext, that is, after it has invoked all of the
     * ServletContextListener objects configured for the WebappContext
     * at their {@link jakarta.servlet.ServletContextListener#contextInitialized}
     * method.
     * <p/>
     * <p>If <tt>loadOnStartup</tt> is a negative integer, the container
     * is free to instantiate and initialize the Servlet lazily.
     * <p/>
     * <p>The default value for <tt>loadOnStartup</tt> is <code>-1</code>.
     * <p/>
     * <p>A call to this method overrides any previous setting.
     *
     * @param loadOnStartup the initialization priority of the Servlet
     * @throws IllegalStateException if the ServletContext from which
     *                               this ServletRegistration was obtained has already been initialized
     */
    @Override
    public void setLoadOnStartup(int loadOnStartup) {
        if (ctx.deployed) {
            throw new IllegalStateException("WebappContext has already been deployed");
        }
        if (loadOnStartup < 0) {
            this.loadOnStartup = -1;
        } else {
            this.loadOnStartup = loadOnStartup;
        }
    }

    /**
     * Get the {@link ExpectationHandler} responsible for processing
     * <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     * 
     * @return the {@link ExpectationHandler} responsible for processing
     * <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     */
    public ExpectationHandler getExpectationHandler() {
        return expectationHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        return Collections.emptySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRunAsRole() {
        return runAs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRunAsRole(String roleName) {
        this.runAs = roleName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.isAsyncSupported = isAsyncSupported;
    }
    
    /**
     * Set the {@link ExpectationHandler} responsible for processing
     * <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     * 
     * @param expectationHandler  the {@link ExpectationHandler} responsible
     * for processing <tt>Expect:</tt> header (for example "Expect: 100-Continue").
     */
    public void setExpectationHandler(ExpectationHandler expectationHandler) {
        this.expectationHandler = expectationHandler;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ServletRegistration");
        sb.append("{ servletName=").append(name);
        sb.append(", servletClass=").append(className);
        sb.append(", urlPatterns=").append(Arrays.toString(urlPatterns.getArray()));
        sb.append(", loadOnStartup=").append(loadOnStartup);
        sb.append(", isAsyncSupported=").append(isAsyncSupported);
        sb.append(" }");
        return sb.toString();
    }


    // ------------------------------------------------- Methods from Comparable


    @Override
    public int compareTo(ServletRegistration o) {
        if (loadOnStartup == o.loadOnStartup) {
            return 0;
        }
        if (loadOnStartup < 0 && o.loadOnStartup < 0) {
            return -1;
        }
        if (loadOnStartup >= 0 && o.loadOnStartup >= 0) {
            if (loadOnStartup < o.loadOnStartup) {
                return -1;
            }
        }
        return 1;
    }
}
