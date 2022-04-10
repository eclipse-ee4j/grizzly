/*
 * Copyright (c) 2022, 2022 Contributors to the Eclipse Foundation
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

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.util.BufferChunk;
import org.glassfish.grizzly.http.util.ByteChunk;
import org.glassfish.grizzly.http.util.CookieHeaderParser;
import org.glassfish.grizzly.http.util.CookieParserUtils;
import org.glassfish.grizzly.http.util.CookieUtils;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.MimeHeaders;

/**
 * A collection of cookies - reusable and tuned for server side performance. Based on RFC2965 ( and 2109 )
 *
 * This class is not synchronized.
 *
 * @author Costin Manolache
 * @author kevin seguin
 */
public final class Cookies {

    private static final Cookie[] EMPTY_COOKIE_ARRAY = new Cookie[0];

    private static final Logger logger = Grizzly.logger(Cookies.class);
    // expected average number of cookies per request
    private static final int INITIAL_SIZE = 4;
    private Cookie[] cookies = new Cookie[INITIAL_SIZE];
    private Cookie[] processedCookies;

    private boolean isProcessed;
    private boolean isRequest;
    private MimeHeaders headers;

    private int nextUnusedCookieIndex = 0;
    private int storedCookieCount;

    /*
     * List of Separator Characters (see isSeparator()) Excluding the '/' char violates the RFC, but it looks like a lot of
     * people put '/' in unquoted values: '/': ; //47 '\t':9 ' ':32 '\"':34 '\'':39 '(':40 ')':41 ',':44 ':':58 ';':59
     * '<':60 '=':61 '>':62 '?':63 '@':64 '[':91 '\\':92 ']':93 '{':123 '}':125
     */
    static final char SEPARATORS[] = { '\t', ' ', '\"', '\'', '(', ')', ',', ':', ';', '<', '=', '>', '?', '@', '[', '\\', ']', '{', '}' };
    static final boolean separators[] = new boolean[128];

    static {
        for (int i = 0; i < 128; i++) {
            separators[i] = false;
        }
        for (int i = 0; i < SEPARATORS.length; i++) {
            separators[SEPARATORS[i]] = true;
        }
    }

    public boolean initialized() {
        return headers != null;
    }

    public Cookie[] get() {
        if (!isProcessed) {
            isProcessed = true;
            if (isRequest) {
                processClientCookies();
            } else {
                processServerCookies();
            }

            processedCookies = nextUnusedCookieIndex > 0 ? copyTo(new Cookie[nextUnusedCookieIndex]) : EMPTY_COOKIE_ARRAY;
        }

        return processedCookies;
    }

    public void setHeaders(final MimeHeaders headers) {
        setHeaders(headers, true);
    }

    public void setHeaders(final MimeHeaders headers, final boolean isRequest) {
        this.headers = headers;
        this.isRequest = isRequest;
    }

    public Cookie getNextUnusedCookie() {

        if (nextUnusedCookieIndex < storedCookieCount) {
            return cookies[nextUnusedCookieIndex++];
        } else {
            Cookie cookie = new Cookie();
            if (nextUnusedCookieIndex == cookies.length) {
                Cookie[] temp = new Cookie[cookies.length + INITIAL_SIZE];
                System.arraycopy(cookies, 0, temp, 0, cookies.length);
                cookies = temp;
            }
            storedCookieCount++;
            cookies[nextUnusedCookieIndex++] = cookie;
            return cookie;
        }
    }

    /**
     * Recycle.
     */
    public void recycle() {
        for (int i = 0; i < nextUnusedCookieIndex; i++) {
            cookies[i].recycle();
        }
        processedCookies = null;
        nextUnusedCookieIndex = 0;
        headers = null;
        isRequest = false;
        isProcessed = false;
    }

    private Cookie[] copyTo(Cookie[] destination) {
        if (nextUnusedCookieIndex > 0) {
            System.arraycopy(cookies, 0, destination, 0, nextUnusedCookieIndex);
        }
        return destination;
    }

    // code from CookieTools
    /**
     * Add all Cookie found in the headers of a request.
     */
    private void processClientCookies() {
        if (headers == null) {
            return;// nothing to process
        } // process each "cookie" header
        int pos = 0;
        while (pos >= 0) {
            // Cookie2: version ? not needed
            pos = headers.indexOf(Header.Cookie, pos);
            // no more cookie headers headers
            if (pos < 0) {
                break;
            }

            DataChunk cookieValue = headers.getValue(pos);
            if (cookieValue == null || cookieValue.isNull()) {
                pos++;
                continue;
            }

            // Uncomment to test the new parsing code
            if (cookieValue.getType() == DataChunk.Type.Bytes) {
                if (logger.isLoggable(Level.FINE)) {
                    log("Parsing b[]: " + cookieValue.toString());
                }

                final ByteChunk byteChunk = cookieValue.getByteChunk();

                if (CookieUtils.USE_LEGACY_PARSER) {
                    CookieParserUtils.parseClientCookies(this, byteChunk.getBuffer(), byteChunk.getStart(), byteChunk.getLength());
                } else {
                    CookieHeaderParser.parseCookie(byteChunk.getBuffer(), byteChunk.getStart(), byteChunk.getLength(), this);
                }
            } else if (cookieValue.getType() == DataChunk.Type.Buffer) {
                if (logger.isLoggable(Level.FINE)) {
                    log("Parsing buffer: " + cookieValue.toString());
                }

                final BufferChunk bufferChunk = cookieValue.getBufferChunk();
                CookieParserUtils.parseClientCookies(this, bufferChunk.getBuffer(), bufferChunk.getStart(), bufferChunk.getLength());
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    log("Parsing string: " + cookieValue.toString());
                }

                final String value = cookieValue.toString();
                CookieParserUtils.parseClientCookies(this, value, CookieUtils.COOKIE_VERSION_ONE_STRICT_COMPLIANCE, CookieUtils.RFC_6265_SUPPORT_ENABLED);
            }

            pos++;// search from the next position
        }
    }

    // code from CookieTools
    /**
     * Add all Cookie found in the headers of a request.
     */
    private void processServerCookies() {
        if (headers == null) {
            return;// nothing to process
        } // process each "cookie" header
        int pos = 0;
        while (pos >= 0) {
            // Cookie2: version ? not needed
            pos = headers.indexOf(Header.SetCookie, pos);
            // no more cookie headers headers
            if (pos < 0) {
                break;
            }

            DataChunk cookieValue = headers.getValue(pos);
            if (cookieValue == null || cookieValue.isNull()) {
                pos++;
                continue;
            }

            // Uncomment to test the new parsing code
            if (cookieValue.getType() == DataChunk.Type.Bytes) {
                if (logger.isLoggable(Level.FINE)) {
                    log("Parsing b[]: " + cookieValue.toString());
                }

                final ByteChunk byteChunk = cookieValue.getByteChunk();
                CookieParserUtils.parseServerCookies(this, byteChunk.getBuffer(), byteChunk.getStart(), byteChunk.getLength(),
                        CookieUtils.COOKIE_VERSION_ONE_STRICT_COMPLIANCE, CookieUtils.RFC_6265_SUPPORT_ENABLED);
            } else if (cookieValue.getType() == DataChunk.Type.Buffer) {
                if (logger.isLoggable(Level.FINE)) {
                    log("Parsing b[]: " + cookieValue.toString());
                }

                final BufferChunk bufferChunk = cookieValue.getBufferChunk();
                CookieParserUtils.parseServerCookies(this, bufferChunk.getBuffer(), bufferChunk.getStart(), bufferChunk.getLength(),
                        CookieUtils.COOKIE_VERSION_ONE_STRICT_COMPLIANCE, CookieUtils.RFC_6265_SUPPORT_ENABLED);
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    log("Parsing string: " + cookieValue.toString());
                }

                final String value = cookieValue.toString();
                CookieParserUtils.parseServerCookies(this, value, CookieUtils.COOKIE_VERSION_ONE_STRICT_COMPLIANCE, CookieUtils.RFC_6265_SUPPORT_ENABLED);
            }

            pos++;// search from the next position
        }
    }

    /**
     * EXPENSIVE!!! only for debugging.
     */
    @Override
    public String toString() {
        return Arrays.toString(cookies);
    }

    private static void log(String s) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Cookies: {0}", s);
        }
    }

    public Cookie findByName(String cookieName) {
        final Cookie[] cookiesArray = get();
        for (Cookie cookie : cookiesArray) {
            if (cookie.lazyNameEquals(cookieName)) {
                return cookie;
            }
        }
        return null;
    }
}
