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

package org.glassfish.grizzly.http.server;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;

/**
 * The {@link HttpServer} addon interface, responsible for adding
 * features like WebSockets, Comet to HttpServer.
 *
 * @author Alexey Stashok
 */
public interface AddOn {
    /**
     * The method, which will be invoked by {@link HttpServer} in order to
     * initialize the AddOn on the passed {@link NetworkListener}.
     * Most of the time the AddOn implementation will update the passed
     * {@link NetworkListener}'s {@link FilterChainBuilder} by adding custom
     * {@link org.glassfish.grizzly.filterchain.Filter}(s), which implement
     * AddOn's logic.
     * 
     * @param networkListener the {@link NetworkListener} the addon is being
     *          initialized on.
     * @param builder the {@link FilterChainBuilder},
     *          representing the {@link NetworkListener} logic.
     */
    void setup(NetworkListener networkListener,
               FilterChainBuilder builder);
}
