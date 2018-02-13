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

package org.glassfish.grizzly.comet;

import java.io.IOException;

import org.glassfish.grizzly.http.server.Response;

/**
 * A basic default implementation of CometHandler to take care of tracking the Response and CometContext.
 */
public class DefaultCometHandler<E> implements CometHandler<E> {
    private Response response;
    private CometContext<E> cometContext;

    public DefaultCometHandler() {
    }

    public DefaultCometHandler(final CometContext<E> cometContext, final Response response) {
        this.cometContext = cometContext;
        this.response = response;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setResponse(final Response response) {
        this.response = response;
    }

    @Override
    public CometContext<E> getCometContext() {
        return cometContext;
    }

    @Override
    public void setCometContext(final CometContext<E> cometContext) {
        this.cometContext = cometContext;
    }

    @Override
    public void onEvent(final CometEvent event) throws IOException {
    }

    @Override
    public void onInitialize(final CometEvent event) throws IOException {
    }

    @Override
    public void onTerminate(final CometEvent event) throws IOException {
    }

    @Override
    public void onInterrupt(final CometEvent event) throws IOException {
    }
}
