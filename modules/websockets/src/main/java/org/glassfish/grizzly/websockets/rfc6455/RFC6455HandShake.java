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

package org.glassfish.grizzly.websockets.rfc6455;

import static org.glassfish.grizzly.websockets.Constants.ORIGIN_HEADER;
import static org.glassfish.grizzly.websockets.Constants.SEC_WS_ORIGIN_HEADER;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.websockets.Constants;
import org.glassfish.grizzly.websockets.HandShake;
import org.glassfish.grizzly.websockets.HandshakeException;
import org.glassfish.grizzly.websockets.SecKey;

public class RFC6455HandShake extends HandShake {

    private final SecKey secKey;
    private final List<String> enabledExtensions = Collections.emptyList();
    private final List<String> enabledProtocols = Collections.emptyList();

    // ------------------------------------------------------------ Constructors

    public RFC6455HandShake(URI uri) {
        super(uri);
        secKey = new SecKey();
    }

    public RFC6455HandShake(HttpRequestPacket request) {
        super(request);
        final MimeHeaders mimeHeaders = request.getHeaders();
        String header = mimeHeaders.getHeader(Constants.SEC_WS_EXTENSIONS_HEADER);
        if (header != null) {
            setExtensions(parseExtensionsHeader(header));
        }
        secKey = SecKey.generateServerKey(new SecKey(mimeHeaders.getHeader(Constants.SEC_WS_KEY_HEADER)));
    }

    // -------------------------------------------------- Methods from HandShake

    @Override
    protected int getVersion() {
        return 13;
    }

    @Override
    public void setHeaders(HttpResponsePacket response) {
        response.setReasonPhrase(Constants.RESPONSE_CODE_MESSAGE);
        response.setHeader(Constants.SEC_WS_ACCEPT, secKey.getSecKey());
        if (!getEnabledExtensions().isEmpty()) {
            response.setHeader(Constants.SEC_WS_EXTENSIONS_HEADER, join(getSubProtocol()));
        }
    }

    @Override
    public HttpContent composeHeaders() {
        HttpContent content = super.composeHeaders();
        final HttpHeader header = content.getHttpHeader();
        header.addHeader(Constants.SEC_WS_KEY_HEADER, secKey.toString());
        header.addHeader(Constants.SEC_WS_ORIGIN_HEADER, getOrigin());
        header.addHeader(Constants.SEC_WS_VERSION, getVersion() + "");
        if (!getExtensions().isEmpty()) {
            header.addHeader(Constants.SEC_WS_EXTENSIONS_HEADER, joinExtensions(getExtensions()));
        }

        final String headerValue = header.getHeaders().getHeader(SEC_WS_ORIGIN_HEADER);
        header.getHeaders().removeHeader(SEC_WS_ORIGIN_HEADER);
        header.addHeader(ORIGIN_HEADER, headerValue);
        return content;
    }

    @Override
    public void validateServerResponse(final HttpResponsePacket headers) throws HandshakeException {
        super.validateServerResponse(headers);
        secKey.validateServerKey(headers.getHeader(Constants.SEC_WS_ACCEPT));
    }

    public List<String> getEnabledExtensions() {
        return enabledExtensions;
    }

    public List<String> getEnabledProtocols() {
        return enabledProtocols;
    }

}
