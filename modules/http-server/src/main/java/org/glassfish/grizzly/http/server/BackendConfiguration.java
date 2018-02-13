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

package org.glassfish.grizzly.http.server;

/**
 * This configuration might be useful, when Grizzly HttpServer is running
 * behind an HTTP gateway like reverse proxy, load balancer etc...
 * 
 * In this situation the HTTP gateway may preprocess initial HTTP request
 * headers, examine them and forward modified HTTP request to a Grizzly HttpServer.
 * For example HTTP request received via HTTPS might be forwarded using plain HTTP
 * (for performance reason), so the protocol and client authentication might
 * be lost when the HTTP request reaches Grizzly HttpServer.
 * Using this configuration object, it's possible to instruct Grizzly HttpServer
 * to use custom HTTP request headers to get information about original protocol
 * used by client, user authentication information etc...
 * 
 * @since 2.3.18
 * 
 * @author Alexey Stashok
 */
public class BackendConfiguration {
    /**
     * The HTTP request scheme, which if non-null overrides default one picked
     * up by framework during runtime.
     */
    private String scheme;

    private String schemeMapping;
    
    private String remoteUserMapping;

    /**
     * Returns the HTTP request scheme, which if non-null overrides default one
     * picked up by framework during request processing.
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Sets the HTTP request scheme, which if non-null overrides default one
     * picked up by framework during request processing.
     * Pls. note this method resets {@link #schemeMapping} property if any
     * was set before.
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
        schemeMapping = null;
    }

    /**
     * Returns the HTTP request header name, whose value (if non-null) would be used
     * to override default protocol scheme picked up by framework during
     * request processing.
     */
    public String getSchemeMapping() {
        return schemeMapping;
    }

    /**
     * Sets the HTTP request header name, whose value (if non-null) would be used
     * to override default protocol scheme picked up by framework during
     * request processing.
     * Pls. note this method resets {@link #scheme} property if any
     * was set before.
     */
    public void setSchemeMapping(String schemeMapping) {
        this.schemeMapping = schemeMapping;
        scheme = null;
    }

    /**
     * Returns the HTTP request header name, whose value (if non-null) would be used
     * to set the name of the remote user that has been authenticated
     * for HTTP Request.
     * 
     * @see Request#getRemoteUser()
     */
    public String getRemoteUserMapping() {
        return remoteUserMapping;
    }

    /**
     * Sets the HTTP request header name, whose value (if non-null) would be used
     * to set the name of the remote user that has been authenticated
     * for HTTP Request.
     * 
     * @see Request#getRemoteUser()
     */
    public void setRemoteUserMapping(String remoteUserMapping) {
        this.remoteUserMapping = remoteUserMapping;
    }
}
