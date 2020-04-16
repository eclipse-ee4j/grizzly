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

package org.glassfish.grizzly.servlet;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler, responsible for processing Expect: header in a HTTP requests,
 * for example "Expect: 100-Continue"
 * 
 * @author Alexey Stashok
 */
public interface ExpectationHandler {
    /**
     * Method is getting called by framework if HTTP request contains "Expect"
     * header.
     * Depending on request information, implementation may decide to accept
     * or refuse the HTTP message payload, using passed {@link AckAction}.
     * Use {@link AckAction#acknowledge()} to confirm expectation, or
     * {@link AckAction#fail()} to refuse it.
     * 
     * @param request {@link HttpServletRequest}
     * @param response {@link HttpServletRequest}
     * @param action {@link AckAction}.
     * 
     * @throws Exception 
     */
    void onExpectAcknowledgement(final HttpServletRequest request,
                                 final HttpServletResponse response, final AckAction action)
            throws Exception;
    
    /**
     * Interface, using which {@link ExpectationHandler} may confirm or refuse
     * client expectation.
     */
    interface AckAction {
        /**
         * Acknowledges a client that server wants to receive payload.
         * 
         * @throws IOException 
         */
        void acknowledge() throws IOException;
        
        /**
         * Notifies a client that server doesn't want to process HTTP message
         * payload.
         * 
         * @throws IOException 
         */
        void fail() throws IOException;
    }
}
