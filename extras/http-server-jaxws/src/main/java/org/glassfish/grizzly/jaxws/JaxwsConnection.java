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

package org.glassfish.grizzly.jaxws;

import com.sun.istack.NotNull;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.PortAddressResolver;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.api.server.WebServiceContextDelegate;
import com.sun.xml.ws.transport.Headers;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.WSHTTPConnection;

import jakarta.xml.ws.handler.MessageContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.*;
import jakarta.xml.ws.WebServiceException;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

/**
 * JAX-WS WSHTTPConnection implementation for grizzly transport
 * 
 * @author Alexey Stashok
 * @author JAX-WS team
 */
final class JaxwsConnection extends WSHTTPConnection implements WebServiceContextDelegate {

    private final Request request;
    private final Response response;
    private int status;
    private boolean outputWritten;
    private final boolean isSecure;
    private final boolean isAsync;
    final HttpAdapter httpAdapter;

    private Headers requestHeaders;
    private Headers responseHeaders;
    
    public JaxwsConnection(final HttpAdapter httpAdapter,
            final Request request, final Response response,
            final boolean isSecure, final boolean isAsync) {
        this.httpAdapter = httpAdapter;
        this.request = request;
        this.response = response;
        this.isSecure = isSecure;
        this.isAsync = isAsync;
    }

    @Override
    @Property({MessageContext.HTTP_REQUEST_HEADERS, Packet.INBOUND_TRANSPORT_HEADERS})
    public @NotNull Map<String, List<String>> getRequestHeaders() {
        if (requestHeaders == null) {
            requestHeaders = new Headers();
            for (String headerName : request.getHeaderNames()) {
                final List<String> headerValues = new ArrayList<String>(4);

                for (String headerValue : request.getHeaders(headerName)) {
                    headerValues.add(headerValue);
                }

                requestHeaders.put(headerName, headerValues);
            }
        }
        
        return requestHeaders;
    }

    @Override
    public String getRequestHeader(String headerName) {
        return request.getHeader(headerName);
    }

    @Override
    public Set<String> getRequestHeaderNames() {
    	return getRequestHeaders().keySet();
    }

    @Override
    public List<String> getRequestHeaderValues(@NotNull String headerName) {
        return getRequestHeaders().get(headerName);
    }

    @Override
    public void setResponseHeaders(Map<String, List<String>> headers) {
        if (headers == null) {
            responseHeaders = null;
        } else {
            if (responseHeaders == null) {
                responseHeaders = new Headers();
            } else {
                responseHeaders.clear();
            }
            responseHeaders.putAll(headers);
        }
    }

    @Override
    @Property(MessageContext.HTTP_RESPONSE_HEADERS)
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public void setResponseHeader(String key, String value) {
    	setResponseHeader(key, Collections.singletonList(value));
    }
    
    @Override
    public void setResponseHeader(String key, List<String> value) {
        if (responseHeaders == null) {
            responseHeaders = new Headers();
        }
    	
        responseHeaders.put(key, value);
    }
    
    @Override
    public String getRequestURI() {
        return request.getRequestURI();
    }

    @Override
    public String getRequestScheme() {
        return request.getScheme();
    }

    @Override
    public String getServerName() {
        return request.getServerName();
    }

    @Override
    public int getServerPort() {
        return request.getServerPort();
    }    
    
    @Override
    public void setContentTypeResponseHeader(@NotNull String value) {
        response.setContentType(value);
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    @Property(MessageContext.HTTP_RESPONSE_CODE)
    public int getStatus() {
        return status;
    }

    @NotNull
    @Override
    public InputStream getInput() throws IOException {
        return request.getInputStream();
    }

    @NotNull
    @Override
    public OutputStream getOutput() throws IOException {
        response.setStatus(status);
        if (responseHeaders != null) {
            for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                String name = entry.getKey();
                if (name == null) {
                    continue;
                }
                if (name.equalsIgnoreCase("Content-Type") || name.equalsIgnoreCase("Content-Length")) {
                    continue;   // ignore headers that interfere with the operation
                }
                for (String value : entry.getValue()) {
                    response.addHeader(name, value);
                }
            }
        }
        
        return response.getOutputStream();
    }

    @NotNull
    @Override
    public WebServiceContextDelegate getWebServiceContextDelegate() {
        return this;
    }

    @Override
    public Principal getUserPrincipal(Packet request) {
        return this.request.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(Packet request, String role) {
        return false;
    }

    @NotNull
    @Override
    public String getEPRAddress(Packet p, WSEndpoint endpoint) {
        PortAddressResolver resolver = httpAdapter.owner.createPortAddressResolver(getBaseAddress(), endpoint.getImplementationClass());
        String address = resolver.getAddressFor(endpoint.getServiceName(), endpoint.getPortName().getLocalPart());
        if (address == null) {
            throw new WebServiceException("WsservletMessages.SERVLET_NO_ADDRESS_AVAILABLE(" + endpoint.getPortName() + ")");
        }
        return address;
    }

    @Override
    public String getWSDLAddress(@NotNull Packet request, @NotNull WSEndpoint endpoint) {
        String eprAddress = getEPRAddress(request, endpoint);
        return eprAddress + "?wsdl";
    }

    @NotNull
    @Override
    public String getBaseAddress() {
        return getBaseAddress(request);
    }

    static @NotNull
    String getBaseAddress(Request request) {
        StringBuilder buf = new StringBuilder();
        buf.append(request.getScheme());
        buf.append("://");
        buf.append(request.getServerName());
        buf.append(':');
        buf.append(request.getServerPort());
        buf.append(request.getContextPath());

        return buf.toString();
    }

    @Override
    public boolean isSecure() {
        return this.isSecure;
    }

    @Override
    @Property(MessageContext.HTTP_REQUEST_METHOD)
    public @NotNull
    String getRequestMethod() {
        return request.getMethod().getMethodString();
    }

    @Override
    @Property(MessageContext.QUERY_STRING)
    public String getQueryString() {
        return request.getQueryString();
    }

    @Override
    @Property(MessageContext.PATH_INFO)
    public String getPathInfo() {
        return request.getPathInfo();
    }

    /**
     * Override the close to make sure the Grizzly ARP processing completes
     * Delegate further processing to parent class
     */
    @Override
    public void close() {
        try {
            super.close();
        } finally {
            if (isAsync) {
                response.resume();
            }
        }
    }

    @Override
    protected PropertyMap getPropertyMap() {
        return model;
    }
    private static final PropertyMap model;

    static {
        model = parse(JaxwsConnection.class);
    }
}
