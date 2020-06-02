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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.MimeType;
import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.glassfish.grizzly.servlet.HttpServletRequestImpl;
import org.glassfish.grizzly.servlet.HttpServletResponseImpl;
import org.osgi.service.http.HttpContext;

/**
 * OSGi Resource {@link HttpHandler}.
 * <p/>
 * OSGi Resource registration integration.
 *
 * @author Hubert Iwaniuk
 */
public class OSGiResourceHandler extends HttpHandler implements OSGiHandler {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final String alias;
    private final String prefix;
    private final HttpContext httpContext;
    private final OSGiServletContext servletContext;
    private final Logger logger;

    /**
     * Default constructor.
     *
     * @param alias Registered under this alias.
     * @param prefix Internal prefix.
     * @param httpContext Backing {@link org.osgi.service.http.HttpContext}.
     * @param logger Logger utility.
     */
    public OSGiResourceHandler(String alias, String prefix, HttpContext httpContext, OSGiServletContext servletContext, Logger logger) {
        super();
        // noinspection AccessingNonPublicFieldOfAnotherObject
//        super.commitErrorResponse = false;
        this.alias = alias;
        this.prefix = prefix;
        this.httpContext = httpContext;
        this.servletContext = servletContext;
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(Request request, Response response) throws Exception {
        String requestURI = request.getDecodedRequestURI();
        logger.debug("OSGiResourceHandler requestURI: " + requestURI);
        String path = requestURI.replaceFirst(alias, prefix);
        try {
            // authentication
            if (!authenticate(request, response, servletContext)) {
                logger.debug("OSGiResourceHandler Request not authenticated (" + requestURI + ").");
                return;
            }
        } catch (IOException e) {
            logger.warn("Error while authenticating request: " + request, e);
        }

        // find resource
        URL resource = httpContext.getResource(path);
        if (resource == null) {
            logger.debug("OSGiResourceHandler \'" + alias + "\' Haven't found '" + path + "'.");
            response.setStatus(404);
            return;
        } else {
            response.setStatus(200);
        }

        // MIME handling
        String mime = httpContext.getMimeType(path);
        if (mime == null) {
            mime = MimeType.getByFilename(path);
        }
        if (mime != null) {
            response.setContentType(mime);
        }

        try {
            final URLConnection urlConnection = resource.openConnection();
            final int length = urlConnection.getContentLength();
            final InputStream is = urlConnection.getInputStream();
            final OutputStream os = response.getOutputStream();

            byte buff[] = new byte[1024 * 8];
            int read, total = 0;
            while ((read = is.read(buff)) != -1) {
                total += read;
                os.write(buff, 0, read);
            }
            os.flush();
            response.finish();
            if (total != length) {
                logger.warn("Was supposed to send " + length + ", but sent " + total);
            }
        } catch (IOException e) {
            logger.warn("", e);
        }
    }

    /**
     * Checks authentication.
     * <p/>
     * Calls {@link HttpContext#handleSecurity} to authenticate.
     *
     * @param request Request to authenticate.
     * @param response Response to populate if authentication not performed but needed.
     * @param servletContext Context needed for proper HttpServletRequest creation.
     * @return <code>true</code> if authenticated and can proceed with processing, else <code>false</code>.
     * @throws IOException Propagate exception thrown by {@link HttpContext#handleSecurity}.
     */
    private boolean authenticate(Request request, Response response, OSGiServletContext servletContext) throws IOException {

        HttpServletRequestImpl servletRequest = new OSGiHttpServletRequest(servletContext);
        HttpServletResponseImpl servletResponse = HttpServletResponseImpl.create();

        servletResponse.initialize(response, servletRequest);
        servletRequest.initialize(request, servletResponse, servletContext);

        return httpContext.handleSecurity(servletRequest, servletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReentrantReadWriteLock.ReadLock getProcessingLock() {
        return lock.readLock();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReentrantReadWriteLock.WriteLock getRemovalLock() {
        return lock.writeLock();
    }

    private static class OSGiHttpServletRequest extends HttpServletRequestImpl {

        public OSGiHttpServletRequest(OSGiServletContext context) throws IOException {
            super();
            setContextImpl(context);
        }
    }
}
