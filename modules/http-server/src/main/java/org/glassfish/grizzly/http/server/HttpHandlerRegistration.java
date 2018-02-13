/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server;

/**
 * Class representing {@link HttpHandler} registration information on a
 * {@link HttpServer}.
 * An instance of the class could be created either from {@link String} using
 * {@link #fromString(java.lang.String)} method, or builder {@link #builder()}.
 * 
 * @author Alexey Stashok
 */
public class HttpHandlerRegistration {
    public static final HttpHandlerRegistration ROOT =
            HttpHandlerRegistration.builder()
            .contextPath("")
            .urlPattern("/")
            .build();
    
    /**
     * @return the <tt>HttpHandlerRegistration</tt> builder.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return the <tt>HttpHandlerRegistration</tt> builder.
     * 
     * @deprecated typo :(
     */
    public static Builder bulder() {
        return builder();
    }

    /**
     * Create a registration from the mapping {@link String}.
     * The part of the <tt>mapping</tt> before the second slash '/' occurrence will
     * be treated as <tt>context-path</tt> and the remainder will be treated as
     * a <tt>url-pattern</tt>.
     * For example:
     *      1) "" will be treated as context-path("") and url-pattern("");
     *      2) "/" will be treated as context-path("") and url-pattern("/");
     *      3) "/a/b/c" will be treated as context-path("/a") and url-pattern("/b/c");
     *      4) "/*" will be treated as context-path("") and url-pattern("/*")
     *      5) "*.jpg" will be treated as context-path("") and url-pattern("*.jpg")
     * 
     * @param mapping the {@link String}
     * @return {@link HttpHandlerRegistration}
     */
    public static HttpHandlerRegistration fromString(final String mapping) {
        if (mapping == null) {
            return ROOT;
        }
        
        final String contextPath = getContextPath(mapping);
        return new HttpHandlerRegistration(contextPath,
                getWrapperPath(contextPath, mapping));
    }
    
    private final String contextPath;
    private final String urlPattern;

    private HttpHandlerRegistration(final String contextPath,
            final String urlPattern) {
        this.contextPath = contextPath;
        this.urlPattern = urlPattern;
    }
    
    /**
     * @return <tt>context-path</tt> part of the registration
     */
    public String getContextPath() {
        return contextPath;
    }
    
    /**
     * @return <tt>url-pattern</tt> part of the registration
     */
    public String getUrlPattern() {
        return urlPattern;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + this.contextPath.hashCode();
        hash = 41 * hash + this.urlPattern.hashCode();
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final HttpHandlerRegistration other = (HttpHandlerRegistration) obj;
        
        return this.contextPath.equals(other.contextPath) &&
                this.urlPattern.equals(other.urlPattern);
    }
    
    private static String getWrapperPath(String ctx, String mapping) {

        if (mapping.indexOf("*.") > 0) {
            return mapping.substring(mapping.lastIndexOf("/") + 1);
        } else if (ctx.length() != 0) {
            return mapping.substring(ctx.length());
        } else if (mapping.startsWith("//")) {
            return mapping.substring(1);
        } else {
            return mapping;
        }
    }

    private static String getContextPath(String mapping) {
        String ctx;
        int slash = mapping.indexOf("/", 1);
        if (slash != -1) {
            ctx = mapping.substring(0, slash);
        } else {
            ctx = mapping;
        }

        if (ctx.startsWith("/*") || ctx.startsWith("*")) {
            ctx = "";
        }

        // Special case for the root context
        if (ctx.equals("/")) {
            ctx = "";
        }

        return ctx;
    }
    
    public static class Builder {
        private String contextPath;
        private String urlPattern;

        public Builder contextPath(final String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        public Builder urlPattern(final String urlPattern) {
            this.urlPattern = urlPattern;
            return this;
        }
        
        public HttpHandlerRegistration build() {
            if (contextPath == null) {
                contextPath = "";
            }
            
            if (urlPattern == null) {
                urlPattern = "/";
            }
            
            return new HttpHandlerRegistration(contextPath, urlPattern);
        }
    }
}
