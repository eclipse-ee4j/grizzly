/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.filterchain.BaseFilter;

/**
 * Base HTTP Filter.
 * Might be extended by Filters, which implement HTTP-based protocols to get finer
 * access to {@link HttpRequestPacket}, {@link HttpResponsePacket} objects.
 * 
 * @author Alexey Stashok
 */
public class HttpBaseFilter extends BaseFilter {
    
    /**
     * Binds {@link HttpRequestPacket} and {@link HttpResponsePacket} objects.
     * 
     * @param request
     * @param response
     * 
     * @see {@link HttpRequestPacket#setResponse(org.glassfish.grizzly.http.HttpResponsePacket)}
     * @see {@link HttpResponsePacket#setRequest(org.glassfish.grizzly.http.HttpRequestPacket)}
     */
    protected void bind (final HttpRequestPacket request,
            final HttpResponsePacket response) {
        request.setResponse(response);
        response.setRequest(request);
    }
}
