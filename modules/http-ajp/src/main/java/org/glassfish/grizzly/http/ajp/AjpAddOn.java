/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.ajp;

import java.util.Properties;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.HttpCodecFilter;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * Ajp {@link AddOn} for the {@link org.glassfish.grizzly.http.server.HttpServer}.
 *
 * The addon searches for {@link HttpCodecFilter} occurrence in the passed {@link FilterChainBuilder}, removes it and
 * adds 2 filters: {@link AjpMessageFilter} and {@link AjpHandlerFilter} on its place.
 *
 * @author Alexey Stashok
 */
public class AjpAddOn implements AddOn {

    private boolean isTomcatAuthentication;
    private String secret;

    public AjpAddOn() {
        isTomcatAuthentication = true;
    }

    /**
     * Construct AjpAddOn
     *
     * @param isTomcatAuthentication if true, the authentication will be done in Grizzly. Otherwise, the authenticated
     * principal will be propagated from the native webserver and used for authorization in Grizzly.
     *
     * @param secret if not null, only requests from workers with this secret keyword will be accepted, or null otherwise.
     */
    public void configure(final boolean isTomcatAuthentication, final String secret) {
        this.isTomcatAuthentication = isTomcatAuthentication;
        this.secret = secret;
    }

    /**
     * Configure Ajp Filter using properties. We support following properties: request.useSecret, request.secret,
     * tomcatAuthentication.
     *
     * @param properties
     */
    public void configure(final Properties properties) {
        if (Boolean.parseBoolean(properties.getProperty("request.useSecret"))) {
            secret = Double.toString(Math.random());
        }

        secret = properties.getProperty("request.secret", secret);
        isTomcatAuthentication = Boolean.parseBoolean(properties.getProperty("tomcatAuthentication", "true"));
    }

    /**
     * If set to true, the authentication will be done in Grizzly. Otherwise, the authenticated principal will be propagated
     * from the native webserver and used for authorization in Grizzly. The default value is true.
     *
     * @return true, if the authentication will be done in Grizzly. Otherwise, the authenticated principal will be
     * propagated from the native webserver and used for authorization in Grizzly.
     */
    public boolean isTomcatAuthentication() {
        return isTomcatAuthentication;
    }

    /**
     * If not null, only requests from workers with this secret keyword will be accepted.
     *
     * @return not null, if only requests from workers with this secret keyword will be accepted, or null otherwise.
     */
    public String getSecret() {
        return secret;
    }

    @Override
    public void setup(final NetworkListener networkListener, final FilterChainBuilder builder) {

        final int httpCodecFilterIdx = builder.indexOfType(HttpCodecFilter.class);
        final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

        int idx;

        if (httpCodecFilterIdx >= 0) {
            builder.remove(httpCodecFilterIdx);
            idx = httpCodecFilterIdx;
        } else {
            idx = httpServerFilterIdx;
        }

        if (idx >= 0) {
            builder.add(idx, createAjpMessageFilter());

            final AjpHandlerFilter ajpHandlerFilter = createAjpHandlerFilter();
            ajpHandlerFilter.setSecret(secret);
            ajpHandlerFilter.setTomcatAuthentication(isTomcatAuthentication);

            builder.add(idx + 1, ajpHandlerFilter);
        }
    }

    protected AjpHandlerFilter createAjpHandlerFilter() {
        return new AjpHandlerFilter();
    }

    protected AjpMessageFilter createAjpMessageFilter() {
        return new AjpMessageFilter();
    }

}
