/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Dictionary;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;

/**
 * An extension to the OSGi {@link HttpService} interface allowing the registration/unregistration of Servlet
 * {@link Filter} instances.
 *
 * @since 2.3.3
 */
public interface HttpServiceExtension extends HttpService {

    /**
     * Registers a {@link Filter} and with the {@link HttpService}.
     *
     * As this is an extension to the standard {@link HttpService} and there are no clear rules on how the mapping of
     * filters should occur, this implementation follows the mapping rules as defined by the Servlet specification.
     *
     * Additionally, it should be noted that the registered {@link Filter}s are effectively associated with a particular
     * {@link HttpContext}. Therefore, if you wish to have multiple filters associated with a particular
     * {@link jakarta.servlet.Servlet}, then you should use the same {@link HttpContext} instance to perform the
     * registration.
     *
     * {@link Filter}s will be invoked in registration order.
     *
     * This method will invoke {@link Filter#init(jakarta.servlet.FilterConfig)} during the registration process.
     *
     * When registering a {@link Filter}, take care not to reuse the same Filter instance across multiple registration
     * invocations. This could cause issues when removing the Filter as it may remove more url matching possibilities than
     * intended.
     *
     * @param filter the {@link Filter} to register.
     * @param urlPattern the url pattern that will invoke this {@link Filter}.
     * @param initParams the initialization params that will be passed to the filter when
     * {@link Filter#init(jakarta.servlet.FilterConfig)} is invoked.
     * @param context the {@link HttpContext} associated with this {@link Filter}.
     *
     * @throws ServletException if an error occurs during {@link Filter} initialization.
     */
    void registerFilter(final Filter filter, final String urlPattern, final Dictionary initParams, final HttpContext context) throws ServletException;

    /**
     * Removes the specified {@link Filter} from the service.
     *
     * @param filter the {@link Filter} to remove.
     */
    void unregisterFilter(final Filter filter);

}
