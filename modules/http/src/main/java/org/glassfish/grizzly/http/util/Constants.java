/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.util;

import java.nio.charset.Charset;
import org.glassfish.grizzly.utils.Charsets;


/**
 * Constants.
 *
 * @author Remy Maucherat
 */
public final class Constants {
    // -------------------------------------------------------------- Constants

    /**
     * CR.
     */
    public static final byte CR = (byte) '\r';


    /**
     * LF.
     */
    public static final byte LF = (byte) '\n';


    /**
     * SP.
     */
    public static final byte SP = (byte) ' ';


    /**
     * HT.
     */
    public static final byte HT = (byte) '\t';


    /**
     * COMMA.
     */
    public static final byte COMMA = (byte) ',';


    /**
     * COLON.
     */
    public static final byte COLON = (byte) ':';


    /**
     * SEMI_COLON.
     */
    public static final byte SEMI_COLON = (byte) ';';


    /**
     * 'A'.
     */
    public static final byte A = (byte) 'A';


    /**
     * 'a'.
     */
    public static final byte a = (byte) 'a';


    /**
     * 'Z'.
     */
    public static final byte Z = (byte) 'Z';


    /**
     * '?'.
     */
    public static final byte QUESTION = (byte) '?';


    /**
     * Lower case offset.
     */
    public static final byte LC_OFFSET = A - a;


    // START SJSAS 6328909
    /**
     * The default response-type
     */
    public final static String DEFAULT_RESPONSE_TYPE =
            /*"text/html; charset=iso-8859-1"*/ null;

    public final static String CHUNKED_ENCODING = "chunked";

    public static final String FORM_POST_CONTENT_TYPE = "application/x-www-form-urlencoded";

    public static final int KEEP_ALIVE_TIMEOUT_IN_SECONDS = 30;
    /**
     * Default max keep-alive count.
     */
    public static final int DEFAULT_MAX_KEEP_ALIVE = 256;

    /**
     * Default HTTP character encoding
     * TODO Grizzly 2.0, by default, parsed the request URI using UTF-8.
     * We should probably do so with query parameters
     */
    public static final String DEFAULT_HTTP_CHARACTER_ENCODING =
            System.getProperty(
                    Constants.class.getName() + ".default-character-encoding",
                    "ISO-8859-1");
    
    /**
     * Default HTTP {@link Charset}
     */
    public static final Charset DEFAULT_HTTP_CHARSET =
            Charsets.lookupCharset(DEFAULT_HTTP_CHARACTER_ENCODING);

    public static final byte[] IDENTITY = "identity".getBytes(DEFAULT_HTTP_CHARSET);
}
