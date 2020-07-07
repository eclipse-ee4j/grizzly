/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.OutputSink;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.filterchain.FilterChainContext;

/**
 * Represents a single logical HTTP transaction. The target storage provided to the constructor provides a way to look
 * up this transaction at any point in the FilterChain execution.
 *
 * @since 2.3
 */
public class HttpContext implements AttributeStorage {

    private static final Attribute<HttpContext> HTTP_CONTEXT_ATTR = AttributeBuilder.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(HttpContext.class.getName());
    private final AttributeStorage contextStorage;
    private final OutputSink outputSink;
    private final Closeable closeable;
    private final HttpRequestPacket request;

    protected HttpContext(final AttributeStorage attributeStorage, final OutputSink outputSink, final Closeable closeable, final HttpRequestPacket request) {
        this.contextStorage = attributeStorage;
        this.closeable = closeable;
        this.outputSink = outputSink;
        this.request = request;
    }

    // ---------------------------------------------------------- Public Methods

    public HttpRequestPacket getRequest() {
        return request;
    }

    public HttpContext attach(final FilterChainContext ctx) {
        HTTP_CONTEXT_ATTR.set(ctx, this);
        return this;
    }

    @Override
    public final AttributeHolder getAttributes() {
        return contextStorage.getAttributes();
    }

    public AttributeStorage getContextStorage() {
        return contextStorage;
    }

    public OutputSink getOutputSink() {
        return outputSink;
    }

    public Closeable getCloseable() {
        return closeable;
    }

    public void close() {
        closeable.closeSilently();
    }

    public static HttpContext newInstance(final AttributeStorage attributeStorage, final OutputSink outputSink, final Closeable closeable,
            final HttpRequestPacket request) {
        return new HttpContext(attributeStorage, outputSink, closeable, request);
    }

    public static HttpContext get(final FilterChainContext ctx) {
        return HTTP_CONTEXT_ATTR.get(ctx);
    }
}
