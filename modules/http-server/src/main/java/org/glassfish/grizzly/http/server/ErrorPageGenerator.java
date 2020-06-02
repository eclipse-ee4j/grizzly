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

package org.glassfish.grizzly.http.server;

/**
 * Error page generator interface, which is responsible for generating of an error page corresponding to the given
 * response status.
 *
 * The <tt>ErrorPageGenerator</tt> is mainly used by {@link Response#sendError(int)} and
 * {@link Response#sendError(int, java.lang.String)} methods.
 *
 * The <tt>ErrorPageGenerator</tt> might be set per 1) {@link HttpServer}: {@link HttpServer#getServerConfiguration()};
 * 2) {@link NetworkListener}:
 * {@link NetworkListener#setDefaultErrorPageGenerator(org.glassfish.grizzly.http.server.ErrorPageGenerator)}; 3)
 * {@link Response}: {@link Response#setErrorPageGenerator(org.glassfish.grizzly.http.server.ErrorPageGenerator)};
 */
public interface ErrorPageGenerator {
    /**
     * Returns the HTML representation of the error page corresponding to the given HTTP response status.
     *
     * @param request {@link Request}
     * @param status response status
     * @param reasonPhrase response reason phrase
     * @param description extra description. Might be <tt>null</tt>
     * @param exception {@link Throwable}, that caused the error. Might be <tt>null</tt>
     * @return
     */
    String generate(final Request request, final int status, final String reasonPhrase, final String description, final Throwable exception);
}
