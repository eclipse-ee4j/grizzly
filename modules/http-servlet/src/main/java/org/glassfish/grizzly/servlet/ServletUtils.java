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

package org.glassfish.grizzly.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * Grizzly Servlet utilities.
 * 
 * @author Alexey Stashok
 */
public class ServletUtils {
    /**
     * Returns internal Grizzly {@link Request} associated with the passed
     * {@link HttpServletRequest}.
     * 
     * @param servletRequest {@link HttpServletRequest}
     * 
     * @throws IllegalArgumentException if passed {@link HttpServletRequest}
     *      is not based on Grizzly {@link Request}.
     * 
     * @return internal Grizzly {@link Request} associated with the passed
     * {@link HttpServletRequest}.
     */
    public static Request getInternalRequest(HttpServletRequest servletRequest) {
        if (servletRequest instanceof Holders.RequestHolder) {
            return ((Holders.RequestHolder) servletRequest).getInternalRequest();
        }
        
        throw new IllegalArgumentException("Passed HttpServletRequest is not based on Grizzly");
    }
    
    /**
     * Returns internal Grizzly {@link Response} associated with the passed
     * {@link HttpServletResponse}.
     * 
     * @param servletResponse {@link HttpServletResponse}
     * 
     * @throws IllegalArgumentException if passed {@link HttpServletResponse}
     *      is not based on Grizzly {@link Response}.
     * 
     * @return internal Grizzly {@link Response} associated with the passed
     * {@link HttpServletResponse}.
     */
    public static Response getInternalResponse(HttpServletResponse servletResponse) {
        if (servletResponse instanceof Holders.ResponseHolder) {
            return ((Holders.ResponseHolder) servletResponse).getInternalResponse();
        }
        
        throw new IllegalArgumentException("Passed HttpServletResponse is not based on Grizzly");
    }
    
}
