/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.Map;

/**
 * Contains auxiliary information about multipart processing.
 * 
 * @author Alexey Stashok
 */
public class MultipartContext {
    public static final String START_ATTR = "start";
    public static final String START_INFO_ATTR = "start-info";
    public static final String TYPE_ATTR = "type";
    public static final String BOUNDARY_ATTR = "boundary";

    private final String contentType;
    private final String boundary;
    private final Map<String, String> contentTypeAttributes;

    public MultipartContext(final String boundary,
            final String contentType,
            final Map<String, String> contentTypeAttributes) {
        this.contentType = contentType;
        this.boundary = boundary;
        this.contentTypeAttributes =
                Collections.unmodifiableMap(contentTypeAttributes);
    }

    /**
     * Get the multipart boundary string.
     * 
     * @return the multipart boundary string.
     */
    public String getBoundary() {
        return boundary;
    }

    /**
     * Returns complete multipart's content-type.
     * 
     * @return complete multipart's content-type.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns non-modifiable map, which contains multipart's content-type attributes.
     * 
     * @return non-modifiable map, which contains multipart's content-type attributes.
     */
    public Map<String, String> getContentTypeAttributes() {
        return contentTypeAttributes;
    }
}
