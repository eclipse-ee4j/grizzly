/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.util.CookieParserUtils;

/**
 * Cookies builder, which could be used to construct a set of cookies, either client or server.
 *
 * @author Alexey Stashok
 */
public class CookiesBuilder {
    /**
     * Returns the client-side cookies builder.
     *
     * @return the client-side cookies builder.
     */
    public static ClientCookiesBuilder client() {
        return client(false, false);
    }

    /**
     * Returns the client-side cookies builder with the specific "strict cookie version compliance".
     *
     * @return the client-side cookies builder with the specific "strict cookie version compliance".
     */
    public static ClientCookiesBuilder client(boolean strictVersionOneCompliant) {
        return new ClientCookiesBuilder(strictVersionOneCompliant, false);
    }

    /**
     * Returns the client-side cookies builder with the specific "strict cookie version compliance".
     *
     * @return the client-side cookies builder with the specific "strict cookie version compliance".
     */
    public static ClientCookiesBuilder client(boolean strictVersionOneCompliant, boolean rfc6265Enabled) {
        return new ClientCookiesBuilder(strictVersionOneCompliant, rfc6265Enabled);
    }

    /**
     * Returns the server-side cookies builder with the specific "strict cookie version compliance".
     *
     * @return the server-side cookies builder with the specific "strict cookie version compliance".
     */
    public static ServerCookiesBuilder server() {
        return server(false, false);
    }

    /**
     * Returns the server-side cookies builder with the specific "strict cookie version compliance".
     *
     * @return the server-side cookies builder with the specific "strict cookie version compliance".
     */
    public static ServerCookiesBuilder server(boolean strictVersionOneCompliant) {
        return new ServerCookiesBuilder(strictVersionOneCompliant, false);
    }

    /**
     * Returns the server-side cookies builder with the specific "strict cookie version compliance".
     *
     * @return the server-side cookies builder with the specific "strict cookie version compliance".
     */
    public static ServerCookiesBuilder server(boolean strictVersionOneCompliant, boolean rfc6265Enabled) {
        return new ServerCookiesBuilder(strictVersionOneCompliant, rfc6265Enabled);
    }

    public static class ClientCookiesBuilder extends AbstractCookiesBuilder<ClientCookiesBuilder> {

        public ClientCookiesBuilder(boolean strictVersionOneCompliant, boolean rfc6265Enabled) {
            super(strictVersionOneCompliant, rfc6265Enabled);
        }

        @Override
        public ClientCookiesBuilder parse(Buffer cookiesHeader) {
            return parse(cookiesHeader, cookiesHeader.position(), cookiesHeader.limit());
        }

        @Override
        public ClientCookiesBuilder parse(Buffer cookiesHeader, int position, int limit) {
            CookieParserUtils.parseClientCookies(cookies, cookiesHeader, position, limit - position, strictVersionOneCompliant, rfc6265Enabled);
            return this;
        }

        @Override
        public ClientCookiesBuilder parse(String cookiesHeader) {
            CookieParserUtils.parseClientCookies(cookies, cookiesHeader, strictVersionOneCompliant, rfc6265Enabled);
            return this;
        }
    }

    public static class ServerCookiesBuilder extends AbstractCookiesBuilder<ServerCookiesBuilder> {

        public ServerCookiesBuilder(boolean strictVersionOneCompliant, boolean rfc6265Enabled) {
            super(strictVersionOneCompliant, rfc6265Enabled);
        }

        @Override
        public ServerCookiesBuilder parse(Buffer cookiesHeader) {
            return parse(cookiesHeader, cookiesHeader.position(), cookiesHeader.limit());
        }

        @Override
        public ServerCookiesBuilder parse(Buffer cookiesHeader, int position, int limit) {
            CookieParserUtils.parseServerCookies(cookies, cookiesHeader, position, limit - position, strictVersionOneCompliant, rfc6265Enabled);
            return this;
        }

        @Override
        public ServerCookiesBuilder parse(String cookiesHeader) {
            CookieParserUtils.parseServerCookies(cookies, cookiesHeader, strictVersionOneCompliant, rfc6265Enabled);
            return this;
        }
    }

    public abstract static class AbstractCookiesBuilder<E extends AbstractCookiesBuilder> {
        protected final boolean strictVersionOneCompliant;
        protected final boolean rfc6265Enabled;

        public AbstractCookiesBuilder(boolean strictVersionOneCompliant, boolean rfc6265Enabled) {
            this.strictVersionOneCompliant = strictVersionOneCompliant;
            this.rfc6265Enabled = rfc6265Enabled;
        }

        protected final Cookies cookies = new Cookies();

        public abstract E parse(Buffer cookiesHeader);

        public abstract E parse(Buffer cookiesHeader, int position, int limit);

        public abstract E parse(String cookiesHeader);

        public Cookies build() {
            return cookies;
        }
    }
}
