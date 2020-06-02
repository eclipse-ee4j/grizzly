/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.http.Cookie;

/**
 * HTTP session manager abstraction responsible for keeping track of HTTP session associated with a user
 * {@link Request}.
 *
 * @since 2.3.17
 */
public interface SessionManager {
    /**
     * Return the session associated with this Request, creating one if necessary and requested.
     *
     * @param request {@link Request}
     * @param requestedSessionId the session id associated with the {@link Request}
     *
     * @return {@link Session}
     */
    Session getSession(final Request request, final String requestedSessionId);

    /**
     * Create a new {@link Session} associated with the {@link Request}.
     *
     * @param request {@link Request}
     * @return a new {@link Session} associated with the {@link Request}
     */
    Session createSession(final Request request);

    /**
     * Change the {@link Session} id and return the original id.
     *
     * @param request {@link Request}
     * @param session {@link Session}
     * @return the old session id
     */
    String changeSessionId(final Request request, final Session session);

    /**
     * Configure session cookie before adding it to the {@link Request#getResponse()}.
     *
     * @param request
     * @param cookie
     */
    void configureSessionCookie(final Request request, final Cookie cookie);

    /**
     * Set the session cookie name that will be used by sessions created by this {@link SessionManager}.
     *
     * @param name the session cookie name
     *
     * @since 2.3.29
     */
    void setSessionCookieName(final String name);

    /**
     * @return the session cookie name
     *
     * @since 2.3.29
     */
    String getSessionCookieName();
}
