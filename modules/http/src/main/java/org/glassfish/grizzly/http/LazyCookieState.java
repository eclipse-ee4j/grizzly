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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.http.util.DataChunk;

/**
 * Lazy cookie implementation, which is based on preparsed Grizzly {@link Buffer}s. The {@link String} representation
 * will be created on demand.
 *
 * Allows recycling and uses Buffer as low-level representation ( and thus the byte-> char conversion can be delayed
 * until we know the charset ).
 *
 * Tomcat.core uses this recyclable object to represent cookies, and the facade will convert it to the external
 * representation.
 */
public class LazyCookieState {
    // Version 0 (Netscape) attributes
    private final DataChunk name = DataChunk.newInstance();
    private final DataChunk value = DataChunk.newInstance();
    // Expires - Not stored explicitly. Generated from Max-Age (see V1)
    private final DataChunk path = DataChunk.newInstance();
    private final DataChunk domain = DataChunk.newInstance();
    private boolean secure;
    // Version 1 (RFC2109) attributes
    private final DataChunk comment = DataChunk.newInstance();

    // Note: Servlet Spec =< 2.5 only refers to Netscape and RFC2109,
    // not RFC2965
    // Version 1 (RFC2965) attributes
    // TODO Add support for CommentURL
    // Discard - implied by maxAge <0
    // TODO Add support for Port
    public LazyCookieState() {
    }

    public void recycle() {
        path.recycle();
        name.recycle();
        value.recycle();
        comment.recycle();
        path.recycle();
        domain.recycle();
        secure = false;
    }

    public DataChunk getComment() {
        return comment;
    }

    public DataChunk getDomain() {
        return domain;
    }

    public DataChunk getPath() {
        return path;
    }

    public void setSecure(boolean flag) {
        secure = flag;
    }

    public boolean getSecure() {
        return secure;
    }

    public DataChunk getName() {
        return name;
    }

    public DataChunk getValue() {
        return value;
    }

    // -------------------- utils --------------------
    @Override
    public String toString() {
        return "LazyCookieState " + getName() + '=' + getValue() + " ; " + ' ' + getPath() + ' ' + getDomain();
    }
}
