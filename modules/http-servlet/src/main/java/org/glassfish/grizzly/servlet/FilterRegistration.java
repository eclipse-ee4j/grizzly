/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;

/**
 * Allows customization of a {@link Filter} registered with the {@link WebappContext}.
 *
 * @since 2.2
 */
public class FilterRegistration extends Registration implements jakarta.servlet.FilterRegistration.Dynamic {

    protected Class<? extends Filter> filterClass;
    protected Filter filter;

    protected boolean isAsyncSupported;

    // ------------------------------------------------------------ Constructors

    /**
     * Creates a new FilterRegistration associated with the specified {@link WebappContext}.
     *
     * @param ctx the owning {@link WebappContext}.
     * @param name the name of the Filter.
     * @param filterClassName the fully qualified class name of the {@link Filter} implementation.
     */
    protected FilterRegistration(final WebappContext ctx, final String name, final String filterClassName) {

        super(ctx, name, filterClassName);
        initParameters = new HashMap<>(4, 1.0f);

    }

    /**
     * Creates a new FilterRegistration associated with the specified {@link WebappContext}.
     *
     * @param ctx the owning {@link WebappContext}.
     * @param name name the name of the Filter.
     * @param filter the class of the {@link Filter} implementation
     */
    protected FilterRegistration(final WebappContext ctx, final String name, final Class<? extends Filter> filter) {
        this(ctx, name, filter.getName());
        this.filterClass = filter;

    }

    /**
     * Creates a new FilterRegistration associated with the specified {@link WebappContext}.
     *
     * @param ctx the owning {@link WebappContext}.
     * @param name name the name of the Filter.
     * @param filter the {@link Filter} instance.
     */
    protected FilterRegistration(final WebappContext ctx, final String name, final Filter filter) {
        this(ctx, name, filter.getClass());
        this.filter = filter;
    }

    // ---------------------------------------------------------- Public Methods

    /**
     * Adds a filter mapping with the given servlet names and dispatcher types for the Filter represented by this
     * FilterRegistration.
     *
     * <p>
     * Filter mappings are matched in the order in which they were added.
     *
     * <p>
     * If this method is called multiple times, each successive call adds to the effects of the former.
     *
     * @param dispatcherTypes the dispatcher types of the filter mapping, or null if the default
     * <tt>DispatcherType.REQUEST</tt> is to be used
     * @param servletNames the servlet names of the filter mapping
     *
     * @throws IllegalArgumentException if <tt>servletNames</tt> is null or empty
     * @throws IllegalStateException if the ServletContext from which this FilterRegistration was obtained has already been
     * initialized
     */
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, String... servletNames) {
        addMappingForServletNames(dispatcherTypes, true, servletNames);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {
        if (ctx.deployed) {
            throw new IllegalStateException("WebappContext has already been deployed");
        }

        if (servletNames == null || servletNames.length == 0) {
            throw new IllegalArgumentException("'servletNames' is null or zero-length");
        }

        for (String servletName : servletNames) {
            FilterMap fmap = new FilterMap();
            fmap.setFilterName(getName());
            fmap.setServletName(servletName);
            fmap.setDispatcherTypes(dispatcherTypes);

            ctx.addFilterMap(fmap, isMatchAfter);
        }
    }

    /**
     * Gets the currently available servlet name mappings of the Filter represented by this <code>FilterRegistration</code>.
     *
     * <p>
     * If permitted, any changes to the returned <code>Collection</code> must not affect this
     * <code>FilterRegistration</code>.
     *
     * @return a (possibly empty) <code>Collection</code> of the currently available servlet name mappings of the Filter
     * represented by this <code>FilterRegistration</code>
     */
    @Override
    public Collection<String> getServletNameMappings() {
        return ctx.getServletNameFilterMappings(getName());
    }

    /**
     * Adds a filter mapping with the given url patterns and dispatcher types for the Filter represented by this
     * FilterRegistration.
     *
     * <p>
     * Filter mappings are matched in the order in which they were added.
     *
     * <p>
     * If this method is called multiple times, each successive call adds to the effects of the former.
     *
     * @param dispatcherTypes the dispatcher types of the filter mapping, or null if the default
     * <tt>DispatcherType.REQUEST</tt> is to be used
     * @param urlPatterns the url patterns of the filter mapping
     *
     * @throws IllegalArgumentException if <tt>urlPatterns</tt> is null or empty
     * @throws IllegalStateException if the ServletContext from which this FilterRegistration was obtained has already been
     * initialized
     */
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, String... urlPatterns) {
        addMappingForUrlPatterns(dispatcherTypes, true, urlPatterns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
        if (ctx.deployed) {
            throw new IllegalStateException("WebappContext has already been deployed");
        }

        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException("'urlPatterns' is null or zero-length");
        }

        for (String urlPattern : urlPatterns) {
            FilterMap fmap = new FilterMap();
            fmap.setFilterName(getName());
            fmap.setURLPattern(urlPattern);
            fmap.setDispatcherTypes(dispatcherTypes);

            ctx.addFilterMap(fmap, isMatchAfter);
        }
    }

    /**
     * Gets the currently available URL pattern mappings of the Filter represented by this <code>FilterRegistration</code>.
     *
     * <p>
     * If permitted, any changes to the returned <code>Collection</code> must not affect this
     * <code>FilterRegistration</code>.
     *
     * @return a (possibly empty) <code>Collection</code> of the currently available URL pattern mappings of the Filter
     * represented by this <code>FilterRegistration</code>
     */
    @Override
    public Collection<String> getUrlPatternMappings() {
        return ctx.getUrlPatternFilterMappings(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.isAsyncSupported = isAsyncSupported;
    }
}
