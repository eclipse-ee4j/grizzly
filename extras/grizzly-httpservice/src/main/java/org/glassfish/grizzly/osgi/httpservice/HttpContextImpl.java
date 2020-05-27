/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.osgi.httpservice;

import java.io.IOException;
import java.net.URL;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Grizzly OSGi {@link HttpService}s {@link HttpContext} implementation.
 *
 * @author Hubert Iwaniuk
 * @since Jan 21, 2009
 */
public class HttpContextImpl implements HttpContext {

    private final Bundle bundle;

    public HttpContextImpl(Bundle bundle) {
        this.bundle = bundle;
    }

    public boolean handleSecurity(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        // By default we allow all :)
        return true;
    }

    @Override
    public URL getResource(String s) {
        return bundle.getResource(s);
    }

    @Override
    public String getMimeType(String s) {
        return null;
    }
}
