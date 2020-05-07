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

import org.osgi.service.http.HttpContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * OSGi Authentication filter.
 *
 * @author Hubert Iwaniuk
 */
public class OSGiAuthFilter implements Filter {
    private final HttpContext httpContext;

    public OSGiAuthFilter(
            final HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    /**
     * {@inheritDoc}
     */
    public void init(final FilterConfig filterConfig) throws ServletException {
        // nothing to do here
    }

    /**
     * OSGi integration. Relies on {@link HttpContext#handleSecurity(HttpServletRequest, HttpServletResponse)}.
     * <p/>
     * If {@link HttpContext#handleSecurity(HttpServletRequest, HttpServletResponse)} returns <code>true</code> proceed
     * to next {@link Filter} in {@link FilterChain}, else do nothing so processing stops here.
     * <p/>
     * {@inheritDoc}
     */
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        if (httpContext.handleSecurity((HttpServletRequest) request, (HttpServletResponse) response)) {
            chain.doFilter(request, response);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        // nothing to do here
    }
}
