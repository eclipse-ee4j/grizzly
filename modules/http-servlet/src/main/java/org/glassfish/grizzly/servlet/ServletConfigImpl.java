/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.glassfish.grizzly.http.server.util.Enumerator;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

/**
 * Basic {@link ServletConfig} implementation.
 *
 * @author Jeanfrancois Arcand
 */
public class ServletConfigImpl implements ServletConfig {

    protected String name;
    protected final ConcurrentMap<String, String> initParameters = new ConcurrentHashMap<>(16, 0.75f, 64);
    protected final WebappContext servletContextImpl;

    protected ServletConfigImpl(WebappContext servletContextImpl) {
        this.servletContextImpl = servletContextImpl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {
        return servletContextImpl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    protected void setInitParameters(Map<String, String> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            this.initParameters.clear();
            this.initParameters.putAll(parameters);
        }
    }

    /**
     * Set the name of this servlet.
     *
     * @param name The new name of this servlet
     */
    public void setServletName(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Enumeration<String> getInitParameterNames() {
        return new Enumerator(initParameters.keySet());
    }
}
