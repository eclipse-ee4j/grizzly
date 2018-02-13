/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.multipart;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.io.NIOInputStream;

/**
 * Entry point for the multipart message processing.
 * Initiates non-blocking, asynchronous multipart message parsing and processing.
 * 
 * @since 2.0.1
 * 
 * @author Alexey Stashok
 * @author Heinrich Schuchardt
 */
public class MultipartScanner {
    public static final String BOUNDARY_ATTR = "boundary";
    
    private static final Logger LOGGER = Grizzly.logger(MultipartScanner.class);

    static final String MULTIPART_CONTENT_TYPE = "multipart";
    
    private MultipartScanner() {
    }
    
    /**
     * Initialize the multipart HTTP request processing.
     * 
     * @param request the multipart HTTP request.
     * @param partHandler the {@link MultipartEntryHandler}, which is responsible
     * for processing multipart entries.
     * @param completionHandler {@link CompletionHandler}, which is invoked after
     * multipart Request has been processed, or error occurred.
     */
    public static void scan(final Request request,
            final MultipartEntryHandler partHandler,
            final CompletionHandler<Request> completionHandler) {
        try {
            final String contentType = request.getContentType();
            if (contentType == null) {
                throw new IllegalStateException("ContentType not found");
            }
            
            final String[] contentTypeParams = contentType.split(";");
            final String[] contentSubType = contentTypeParams[0].split("/");

            if (contentSubType.length != 2
                    || !MULTIPART_CONTENT_TYPE.equalsIgnoreCase(contentSubType[0])) {
                throw new IllegalStateException("Not multipart request");
            }

            String boundary = null;
            final Map<String, String> contentTypeProperties =
                    new HashMap<String, String>();
            
            for (int i = 1; i < contentTypeParams.length; i++) {
                final String param = contentTypeParams[i].trim();
                final String[] paramValue = param.split("=", 2);
                if (paramValue.length == 2) {
                    String key = paramValue[0].trim();
                    String value = paramValue[1].trim();
                    if (value.charAt(0) == '"') {
                        value = value.substring(1,
                                value.length()
                                - 1);
                    }
                    contentTypeProperties.put(key, value);
                    if (BOUNDARY_ATTR.equals(key)) {
                        boundary = value;
                    }
                }
            }

            if (boundary == null) {
                throw new IllegalStateException("Boundary not found");
            }

            final NIOInputStream nioInputStream = request.getNIOInputStream();

            nioInputStream.notifyAvailable(new MultipartReadHandler(request,
                    partHandler, completionHandler,
                    new MultipartContext(boundary, contentType,
                    contentTypeProperties)));
        } catch (Exception e) {
            if (completionHandler != null) {
                completionHandler.failed(e);
            } else {
                LOGGER.log(Level.WARNING, "Error occurred, but no CompletionHandler installed to handle it", e);
            }
        }
    }

    /**
     * Initialize the multipart/mixed {@link MultipartEntry} processing.
     * 
     * @param multipartMixedEntry the multipart/mixed {@link MultipartEntry}.
     * @param partHandler the {@link MultipartEntryHandler}, which is responsible
     * for processing multipart sub-entries.
     * @param completionHandler {@link CompletionHandler}, which is invoked after
     * multipart/mixed {@link MultipartEntry} has been processed, or error occurred.
     */
    public static void scan(final MultipartEntry multipartMixedEntry,
            final MultipartEntryHandler partHandler,
            final CompletionHandler<MultipartEntry> completionHandler) {
        try {
            final String contentType = multipartMixedEntry.getContentType();
            final String[] contentTypeParams = contentType.split(";");
            final String[] contentSubType = contentTypeParams[0].split("/");

            if (contentSubType.length != 2
                    || !MULTIPART_CONTENT_TYPE.equalsIgnoreCase(contentSubType[0])) {
                throw new IllegalStateException("Not multipart request");
            }

            String boundary = null;
            final Map<String, String> contentTypeProperties =
                    new HashMap<String, String>();
            
            for (int i = 1; i < contentTypeParams.length; i++) {
                final String param = contentTypeParams[i].trim();
                final String[] paramValue = param.split("=", 2);
                if (paramValue.length == 2) {
                    String key = paramValue[0].trim();
                    String value = paramValue[1].trim();
                    if (value.charAt(0) == '"') {
                        value = value.substring(1,
                                value.length()
                                - 1);
                    }
                    contentTypeProperties.put(key, value);
                    if (BOUNDARY_ATTR.equals(key)) {
                        boundary = value;
                    }
                }
            }

            if (boundary == null) {
                throw new IllegalStateException("Boundary not found");
            }

            final NIOInputStream nioInputStream = multipartMixedEntry.getNIOInputStream();

            nioInputStream.notifyAvailable(new MultipartReadHandler(multipartMixedEntry,
                    partHandler, completionHandler,
                    new MultipartContext(boundary, contentType,
                    contentTypeProperties)));
        } catch (Exception e) {
            if (completionHandler != null) {
                completionHandler.failed(e);
            } else {
                LOGGER.log(Level.WARNING, "Error occurred, but no CompletionHandler installed to handle it", e);
            }
        }
    }
}
