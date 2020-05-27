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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import com.sun.istack.Nullable;
import com.sun.xml.stream.buffer.XMLStreamBufferResult;
import com.sun.xml.ws.api.BindingID;
import com.sun.xml.ws.api.server.InstanceResolver;
import com.sun.xml.ws.api.server.SDDocumentSource;
import com.sun.xml.ws.api.server.WSEndpoint;
import com.sun.xml.ws.binding.BindingImpl;
import com.sun.xml.ws.server.EndpointFactory;
import com.sun.xml.ws.server.ServerRtException;
import com.sun.xml.ws.transport.http.HttpAdapter;
import com.sun.xml.ws.transport.http.WSHTTPConnection;
import com.sun.xml.ws.util.xml.XmlUtil;

import jakarta.xml.ws.Endpoint;

/**
 * JAX-WS {@link HttpHandler} implementation.
 *
 * @author Alexey Stashok
 * @author JAX-WS team
 */

public class JaxwsHandler extends HttpHandler {

    private static final Logger LOGGER = Grizzly.logger(JaxwsHandler.class);

    private final List<Source> metadata;
    private final Map<String, Object> properties;

    private final Object implementor;
    private final boolean isAsync;

    private WSEndpoint endpoint;
    private HttpAdapter httpAdapter;

    private volatile long timeoutMillis = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);

    /**
     * Create JaxwsHandler based on WebService implementation class, which will operate in synchronous mode.
     *
     * @param implementor WebService implementation.
     */
    public JaxwsHandler(final Object implementor) {
        this(implementor, false);
    }

    /**
     * Create JaxwsHandler based on WebService implementation class.
     *
     * @param implementor WebService implementation class.
     * @param isAsync if <tt>true</tt> the handler will execute WebService in asynchronous mode, otherwise synchronous.
     */
    public JaxwsHandler(final Object implementor, final boolean isAsync) {
        this(implementor, isAsync, null, null);
    }

    /**
     * Create JaxwsHandler based on WebService implementation class.
     *
     * @param implementor WebService implementation class.
     * @param isAsync if <tt>true</tt> the handler will execute WebService in asynchronous mode, otherwise synchronous.
     * @param metadata Other documents that become {@link com.sun.xml.ws.api.server.SDDocument}s. Can be null.
     * @param properties extra properties to be used, when constructing WebService {@link WSEndpoint}.
     */
    public JaxwsHandler(final Object implementor, final boolean isAsync, final List<Source> metadata, final Map<String, Object> properties) {
        this.implementor = implementor;
        this.isAsync = isAsync;
        this.metadata = metadata;
        this.properties = properties;
    }

    /**
     * Create JaxwsHandler based on {@link WSEndpoint}, which will operate in synchronous mode.
     *
     * @param endpoint {@link WSEndpoint}.
     */
    public JaxwsHandler(final WSEndpoint endpoint) {
        this(endpoint, false);
    }

    /**
     * Create JaxwsHandler based on {@link WSEndpoint}, which will operate in synchronous mode.
     *
     * @param endpoint {@link WSEndpoint}.
     * @param isAsync if <tt>true</tt> the handler will execute WebService in asynchronous mode, otherwise synchronous.
     */
    public JaxwsHandler(final WSEndpoint endpoint, final boolean isAsync) {
        this.endpoint = endpoint;
        this.isAsync = isAsync;
        this.implementor = null;
        this.metadata = null;
        this.properties = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        if (implementor != null) {
            this.endpoint = WSEndpoint.create((Class<?>) implementor.getClass(), true, InstanceResolver.createSingleton(implementor).createInvoker(),
                    getProperty(QName.class, Endpoint.WSDL_SERVICE), getProperty(QName.class, Endpoint.WSDL_PORT), null /* no container */,
                    BindingImpl.create(BindingID.parse(implementor.getClass())), getPrimaryWsdl(implementor), buildDocList(), (EntityResolver) null, !isAsync);
        }

        this.httpAdapter = HttpAdapter.createAlone(endpoint);
    }

    public boolean isAsync() {
        return isAsync;
    }

    public void setAsyncTimeout(final long timeout, final TimeUnit timeUnit) {
        timeoutMillis = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
    }

    public long getAsyncTimeout(final TimeUnit timeUnit) {
        return timeUnit.convert(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Main entry point of the {@link HttpHandler} to service a request
     * 
     * @param req incoming HTTP request
     * @param res HTTP response to prepare
     */
    @Override
    public void service(Request req, Response res) throws Exception {
        LOGGER.log(Level.FINE, "Received a request. The request thread {0} .", Thread.currentThread());
        // TODO: synchornous execution for ?wsdl, non AsyncProvider requests
        final WSHTTPConnection connection = new JaxwsConnection(httpAdapter, req, res, req.isSecure(), isAsync);

        if (Method.GET.equals(req.getMethod())) {
            // metadata query. let the interceptor run
            if (isMetadataQuery(connection.getQueryString())) {
                // Sends published WSDL and schema documents as the default action.
                httpAdapter.publishWSDL(connection);
                return;
            }
        }

        if (isAsync) {
            res.suspend(timeoutMillis, TimeUnit.MILLISECONDS, new EmptyCompletionHandler<Response>() {

                @Override
                public void cancelled() {
                    connection.close();
                }

            });
            httpAdapter.invokeAsync(connection);
        } else {
            httpAdapter.handle(connection);
        }

        LOGGER.log(Level.FINE, "Getting out of service(). Done with the request thread {0} .", Thread.currentThread());
    }

    /**
     * Returns true if the given query string is for metadata request.
     *
     * @param query String like "xsd=1" or "perhaps=some&amp;unrelated=query". Can be null.
     * @return true for metadata requests false for web service requests
     */
    private boolean isMetadataQuery(String query) {
        // we intentionally return true even if documents don't exist,
        // so that they get 404.
        return query != null && (query.equals("WSDL") || query.startsWith("wsdl") || query.startsWith("xsd="));
    }

    private <T> T getProperty(Class<T> type, String key) {
        if (properties == null) {
            return null;
        }

        Object o = properties.get(key);
        if (o == null) {
            return null;
        }
        if (type.isInstance(o)) {
            return type.cast(o);
        } else {
            throw new IllegalArgumentException("Property " + key + " has to be of type " + type); // i18n
        }
    }

    /**
     * Gets WSDL from @WebService or @WebServiceProvider
     */
    private @Nullable SDDocumentSource getPrimaryWsdl(final Object implementor) {
        Class implType = implementor.getClass();
        // Takes care of @WebService, @WebServiceProvider's wsdlLocation
        EndpointFactory.verifyImplementorClass(implType);
        String wsdlLocation = EndpointFactory.getWsdlLocation(implType);
        if (wsdlLocation != null) {
            ClassLoader cl = implType.getClassLoader();
            URL url = cl.getResource(wsdlLocation);
            if (url != null) {
                return SDDocumentSource.create(url);
            }
            throw new ServerRtException("cannot.load.wsdl", wsdlLocation);
        }
        return null;
    }

    /**
     * Convert metadata sources using identity transform. So that we can reuse the Source object multiple times.
     */
    private List<SDDocumentSource> buildDocList() {
        List<SDDocumentSource> r = new ArrayList<>();

        if (metadata != null) {
            for (Source source : metadata) {
                try {
                    XMLStreamBufferResult xsbr = XmlUtil.identityTransform(source, new XMLStreamBufferResult());
                    String systemId = source.getSystemId();

                    r.add(SDDocumentSource.create(new URL(systemId), xsbr.getXMLStreamBuffer()));
                } catch (TransformerException te) {
                    throw new ServerRtException("server.rt.err", te);
                } catch (IOException te) {
                    throw new ServerRtException("server.rt.err", te);
                } catch (SAXException e) {
                    throw new ServerRtException("server.rt.err", e);
                } catch (ParserConfigurationException e) {
                    throw new ServerRtException("server.rt.err", e);
                }
            }
        }

        return r;
    }
}
