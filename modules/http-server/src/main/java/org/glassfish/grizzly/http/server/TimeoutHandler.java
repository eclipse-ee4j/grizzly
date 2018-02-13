/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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
 * Handler, which processes suspended {@link Response} timeout and is able
 * to either confirm it or reset.
 * 
 * @author Alexey Stashok
 */
public interface TimeoutHandler {
    /**
     * Method is called, when suspended {@link Response} timeout expired. The custom
     * implementation may decide to confirm timeout and return <tt>true</tt>, so
     * the {@link Response} will be canceled immediately after that, or return
     * <tt>false</tt> to reset the timeout and give more time for processing.
     *
     * @param response {@link Response}.
     * @return <tt>true</tt> to cancel the {@link Response} processing, or
     * <tt>false</tt> to reset the timeout.
     */
    boolean onTimeout(final Response response);
}
