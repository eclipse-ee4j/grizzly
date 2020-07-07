/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.http.server.util;

import org.glassfish.grizzly.http.util.DataChunk;

/**
 * Mapping data.
 */
public class MappingData {

    private static final String CONTEXT_DESC = "context";
    private static final String DEFAULT_DESC = "default";
    private static final String EXACT_DESC = "exact";
    private static final String EXTENSION_DESC = "extension";
    private static final String PATH_DESC = "path";
    private static final String UNKNOWN_DESC = "unknown";

    public static final byte CONTEXT_ROOT = 0x1;
    public static final byte DEFAULT = 0x2;
    public static final byte EXACT = 0x4;
    public static final byte EXTENSION = 0x8;
    public static final byte PATH = 0x10;
    public static final byte UNKNOWN = 0x20;

    public byte mappingType = UNKNOWN;
    public Object host = null;
    public Object context = null;
    public Object wrapper = null;
    public String servletName = null;
    public String descriptorPath = null;
    public String matchedPath = null;
    public boolean jspWildCard = false;
    // START GlassFish 1024
    public boolean isDefaultContext = false;
    // END GlassFish 1024
    public final DataChunk contextPath = DataChunk.newInstance();
    public final DataChunk requestPath = DataChunk.newInstance();
    public final DataChunk wrapperPath = DataChunk.newInstance();
    public final DataChunk pathInfo = DataChunk.newInstance();
    public final DataChunk redirectPath = DataChunk.newInstance();

    public final DataChunk tmpMapperDC = DataChunk.newInstance();

    public void recycle() {
        mappingType = UNKNOWN;
        host = null;
        context = null;
        wrapper = null;
        servletName = null;
        pathInfo.recycle();
        requestPath.recycle();
        wrapperPath.recycle();
        contextPath.recycle();
        redirectPath.recycle();
        jspWildCard = false;
        // START GlassFish 1024
        isDefaultContext = false;
        // END GlassFish 1024
        descriptorPath = null;
        matchedPath = null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("host: ").append(host);
        sb.append("\ncontext: ").append(context);
        sb.append("\nwrapper: ").append(wrapper);
        sb.append("\nservletName: ").append(servletName);
        sb.append("\ncontextPath: ").append(contextPath);
        sb.append("\nrequestPath: ").append(requestPath);
        sb.append("\nwrapperPath: ").append(wrapperPath);
        sb.append("\npathInfo: ").append(pathInfo);
        sb.append("\nredirectPath: ").append(redirectPath);
        sb.append("\nmappingType: ").append(getMappingDescription());
        sb.append("\ndescriptorPath: ").append(descriptorPath);
        sb.append("\nmatchedPath: ").append(matchedPath);
        return sb.toString();
    }

    // -------------------------------------------------------- Private Methods

    private String getMappingDescription() {
        switch (mappingType) {
        case CONTEXT_ROOT:
            return CONTEXT_DESC;
        case DEFAULT:
            return DEFAULT_DESC;
        case EXACT:
            return EXACT_DESC;
        case EXTENSION:
            return EXTENSION_DESC;
        case PATH:
            return PATH_DESC;
        default:
            return UNKNOWN_DESC;
        }
    }

}
