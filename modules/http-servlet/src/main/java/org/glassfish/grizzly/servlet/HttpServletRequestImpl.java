/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.servlet;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.Session;
import org.glassfish.grizzly.http.server.TimeoutHandler;
import org.glassfish.grizzly.http.server.util.Enumerator;
import org.glassfish.grizzly.http.server.util.Globals;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.localization.LogMessages;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestAttributeEvent;
import jakarta.servlet.ServletRequestAttributeListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.MappingMatch;
import jakarta.servlet.http.Part;
import jakarta.servlet.http.PushBuilder;
import jakarta.servlet.http.WebConnection;

/**
 * Facade class that wraps a {@link Request} request object. All methods are delegated to the wrapped request.
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @author Jean-Francois Arcand
 * @version $Revision: 1.7 $ $Date: 2007/08/01 19:04:28 $
 */
public class HttpServletRequestImpl implements HttpServletRequest, Holders.RequestHolder {
    private static final Logger LOGGER = Grizzly.logger(HttpServletRequestImpl.class);

    // ----------------------------------------------- Class/Instance Variables

    /**
     * The wrapped request.
     */
    protected Request request = null;

    /**
     * The corresponding servlet response
     */
    protected HttpServletResponseImpl servletResponse;

    /**
     * Async operation
     */
    // Async mode is supported by default for a request, unless the request
    // has passed a filter or servlet that does not support async
    // operation, in which case async operation will be disabled
    private boolean isAsyncSupported = true;
    private final AtomicBoolean asyncStarted = new AtomicBoolean();
    private AsyncContextImpl asyncContext;
    // Has AsyncContext.complete been called?
    private boolean isAsyncComplete;
    private Thread asyncStartedThread;

    private final ServletInputStreamImpl inputStream;
    private ServletReaderImpl reader;

    private HttpSessionImpl httpSession = null;

    private WebappContext contextImpl;

    private String contextPath = "";
    private String servletPath = "";

    private String pathInfo;

    /**
     * Using stream flag.
     */
    protected boolean usingInputStream = false;

    /**
     * Using writer flag.
     */
    protected boolean usingReader = false;

    /*
     * An upgrade request is received
     */
    private boolean upgrade = false;

    /*
     * The HttpUpgradeHandler to be used for upgrade request
     */
    private HttpUpgradeHandler httpUpgradeHandler;

    /**
     * Mapping that invoked this request.
     */
    private HttpServletMapping httpServletMapping;

    private static final ThreadCache.CachedTypeIndex<HttpServletRequestImpl> CACHE_IDX = ThreadCache.obtainIndex(HttpServletRequestImpl.class, 2);

    // ------------- Factory ----------------
    public static HttpServletRequestImpl create() {
        final HttpServletRequestImpl request = ThreadCache.takeFromCache(CACHE_IDX);
        if (request != null) {
            return request;
        }

        return new HttpServletRequestImpl();
    }

    // ----------------------------------------------------------- Constructors

    /**
     * Construct a wrapper for the specified request.
     */
    protected HttpServletRequestImpl() {
        this.inputStream = new ServletInputStreamImpl(this);
    }

    public void initialize(final Request request, final HttpServletResponseImpl servletResponse, final WebappContext context) throws IOException {
        this.request = request;
        this.servletResponse = servletResponse;
        inputStream.initialize();
        contextImpl = context;
        if (context.getRequestCharacterEncoding() != null) {
            request.setCharacterEncoding(context.getResponseCharacterEncoding());
        }
        if (context.getResponseCharacterEncoding() != null) {
            servletResponse.setCharacterEncoding(context.getResponseCharacterEncoding());
        }
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Prevent cloning the facade.
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    // ------------------------------------------------- ServletRequest Methods

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getAttribute(name);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getAttributeNames() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetAttributePrivilegedAction());
        } else {
            return new Enumerator(request.getAttributeNames());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getCharacterEncoding() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetCharacterEncodingPrivilegedAction());
        } else {
            return request.getCharacterEncoding();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCharacterEncoding(String env) throws java.io.UnsupportedEncodingException {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        request.setCharacterEncoding(env);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getContentLength() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getContentLengthLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContentType() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getContentType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (usingReader) {
            throw new IllegalStateException("Illegal attempt to call getInputStream() after getReader() has already been called.");
        }

        usingInputStream = true;

        return inputStream;
    }

    void recycle() {
        request = null;
        servletResponse = null;
        reader = null;

        inputStream.recycle();

        usingInputStream = false;
        usingReader = false;

        upgrade = false;
        httpUpgradeHandler = null;

        /*
         * Clear and reinitialize all async related instance vars
         */
        if (asyncContext != null) {
            asyncContext.clear();
            asyncContext = null;
        }
        isAsyncSupported = true;
        asyncStarted.set(false);
        isAsyncComplete = false;
        asyncStartedThread = null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public String getParameter(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetParameterPrivilegedAction(name));
        } else {
            return request.getParameter(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getParameterNames() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return new Enumerator(AccessController.doPrivileged(new GetParameterNamesPrivilegedAction()));
        } else {
            return new Enumerator(request.getParameterNames());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public String[] getParameterValues(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        String[] ret;

        /*
         * Clone the returned array only if there is a security manager in place, so that performance won't suffer in the
         * nonsecure case
         */
        if (System.getSecurityManager() != null) {
            ret = AccessController.doPrivileged(new GetParameterValuePrivilegedAction(name));
            if (ret != null) {
                ret = ret.clone();
            }
        } else {
            ret = request.getParameterValues(name);
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String[]> getParameterMap() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetParameterMapPrivilegedAction());
        } else {
            return request.getParameterMap();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getProtocol() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getProtocol().getProtocolString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getScheme();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServerName() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getServerName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getServerPort() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getServerPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (usingInputStream) {
            throw new IllegalStateException("Illegal attempt to call getReader() after getInputStream() has already been called.");
        }

        usingReader = true;
        // inputBuffer.checkConverter();
        if (reader == null) {
            reader = new ServletReaderImpl(request.getReader());
        }

        return reader;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteAddr() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getRemoteAddr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteHost() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getRemoteHost();
    }

    @Override
    public void setAttribute(String name, Object value) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        Object oldValue = request.getAttribute(name);
        request.setAttribute(name, value);

        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length == 0) {
            return;
        }
        ServletRequestAttributeEvent event = null;

        for (EventListener eventListener : listeners) {
            if (!(eventListener instanceof ServletRequestAttributeListener)) {
                continue;
            }
            ServletRequestAttributeListener listener = (ServletRequestAttributeListener) eventListener;
            try {
                if (event == null) {
                    if (oldValue != null) {
                        event = new ServletRequestAttributeEvent(contextImpl, this, name, oldValue);
                    } else {
                        event = new ServletRequestAttributeEvent(contextImpl, this, name, value);
                    }
                }
                if (oldValue != null) {
                    listener.attributeReplaced(event);
                } else {
                    listener.attributeAdded(event);
                }
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_ATTRIBUTE_LISTENER_ADD_ERROR("ServletRequestAttributeListener",
                            listener.getClass().getName()), t);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAttribute(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }
        Object value = request.getAttribute(name);
        request.removeAttribute(name);

        // Notify interested application event listeners
        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length == 0) {
            return;
        }
        ServletRequestAttributeEvent event = null;
        for (EventListener listener2 : listeners) {
            if (!(listener2 instanceof ServletRequestAttributeListener)) {
                continue;
            }
            ServletRequestAttributeListener listener = (ServletRequestAttributeListener) listener2;
            try {
                if (event == null) {
                    event = new ServletRequestAttributeEvent(contextImpl, this, name, value);
                }
                listener.attributeRemoved(event);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_ATTRIBUTE_LISTENER_REMOVE_ERROR("ServletRequestAttributeListener",
                        listener.getClass().getName()), t);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Locale getLocale() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetLocalePrivilegedAction());
        } else {
            return request.getLocale();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Locale> getLocales() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetLocalesPrivilegedAction());
        } else {
            return new Enumerator(request.getLocales());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSecure() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.isSecure();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public RequestDispatcher getRequestDispatcher(String path) {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetRequestDispatcherPrivilegedAction(path));
        } else {
            return getRequestDispatcherInternal(path);
        }
    }

    private RequestDispatcher getRequestDispatcherInternal(String path) {
        if (contextImpl == null) {
            return null;
        }

        // If the path is already context-relative, just pass it through
        if (path == null) {
            return null;
        } else if (path.startsWith("/")) {
            return contextImpl.getRequestDispatcher(path);
        }

        // Convert a request-relative path to a context-relative one
        String servletPath = (String) getAttribute(DispatcherConstants.INCLUDE_SERVLET_PATH);
        if (servletPath == null) {
            servletPath = getServletPath();
        }

        // Add the path info, if there is any
        String pathInfo = getPathInfo();
        String requestPath;

        if (pathInfo == null) {
            requestPath = servletPath;
        } else {
            requestPath = servletPath + pathInfo;
        }

        int pos = requestPath.lastIndexOf('/');
        String relative;
        if (pos >= 0) {
            relative = requestPath.substring(0, pos + 1) + path;
        } else {
            relative = requestPath + path;
        }

        return contextImpl.getRequestDispatcher(relative);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAuthType() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getAuthType();
    }

    @SuppressWarnings("unchecked")
    public Cookie[] getGrizzlyCookies() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        Cookie[] ret;

        /*
         * Clone the returned array only if there is a security manager in place, so that performance won't suffer in the
         * nonsecure case
         */
        if (System.getSecurityManager() != null) {
            ret = AccessController.doPrivileged(new GetCookiesPrivilegedAction());
            if (ret != null) {
                ret = ret.clone();
            }
        } else {
            ret = request.getCookies();
        }

        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDateHeader(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getDateHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHeader(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getHeaders(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetHeadersPrivilegedAction(name));
        } else {
            return new Enumerator(request.getHeaders(name).iterator());
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<String> getHeaderNames() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(new GetHeaderNamesPrivilegedAction());
        } else {
            return new Enumerator(request.getHeaderNames().iterator());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIntHeader(String name) {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getIntHeader(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMethod() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getMethod().getMethodString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathInfo() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return pathInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPathTranslated() {
        if (getPathInfo() == null) {
            return null;
        } else {
            return contextImpl.getRealPath(getPathInfo());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContextPath() {
        return contextPath;
    }

    protected void setContextPath(String contextPath) {
        if (contextPath == null) {
            this.contextPath = "";
        } else {
            this.contextPath = contextPath;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueryString() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getQueryString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteUser() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getRemoteUser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUserInRole(String role) {
        throw new IllegalStateException("Not yet implemented");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.security.Principal getUserPrincipal() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getUserPrincipal();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestedSessionId() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getRequestedSessionId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestURI() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getRequestURI();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringBuffer getRequestURL() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return Request.appendRequestURL(request, new StringBuffer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletPath() {
        return servletPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSession getSession(boolean create) {
        if (httpSession != null && httpSession.isValid()) {
            return httpSession;
        }

        httpSession = null;

        final Session internalSession = request.getSession(create);

        if (internalSession == null) {
            return null;
        }

        internalSession.setSessionTimeout(MILLISECONDS.convert(contextImpl.sessionTimeoutInSeconds, SECONDS));

        httpSession = new HttpSessionImpl(contextImpl, internalSession);

        if (httpSession.isNew()) {
            httpSession.notifyNew();
        }

        return httpSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpSession getSession() {

        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return getSession(true);
    }

    @Override
    public String changeSessionId() {
        final String oldSessionId = request.changeSessionId();
        getSession(false); // to make sure httpSession is not null
        httpSession.notifyIdChanged(oldSessionId);

        return oldSessionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdValid() {
        return request.isRequestedSessionIdValid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return request.isRequestedSessionIdFromCookie();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRequestedSessionIdFromURL() {
        return request.isRequestedSessionIdFromURL();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public jakarta.servlet.http.Cookie[] getCookies() {
        final Cookie[] internalCookies = request.getCookies();
        if (internalCookies == null) {
            return null;
        }

        int cookieIdx = 0;
        jakarta.servlet.http.Cookie[] cookies = new jakarta.servlet.http.Cookie[internalCookies.length];
        for (Cookie cookie : internalCookies) {
            if (cookie instanceof CookieWrapper) {
                cookies[cookieIdx++] = ((CookieWrapper) cookie).getWrappedCookie();
            } else {
                try {
                    jakarta.servlet.http.Cookie currentCookie = new jakarta.servlet.http.Cookie(cookie.getName(), cookie.getValue());
                    currentCookie.setComment(cookie.getComment());
                    if (cookie.getDomain() != null) {
                        currentCookie.setDomain(cookie.getDomain());
                    }
                    currentCookie.setMaxAge(cookie.getMaxAge());
                    currentCookie.setPath(cookie.getPath());
                    currentCookie.setSecure(cookie.isSecure());
                    currentCookie.setVersion(cookie.getVersion());
                    cookies[cookieIdx++] = currentCookie;
                } catch (IllegalArgumentException iae) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING, LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_COOKIE_CREATE_ERROR(cookie.getName(), iae.getLocalizedMessage()));
                    }
                }
            }
        }

        return cookieIdx == cookies.length ? cookies : Arrays.copyOf(cookies, cookieIdx);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRemotePort() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getRemotePort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalName() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getLocalName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalAddr() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getLocalAddr();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalPort() {
        if (request == null) {
            throw new IllegalStateException("Null request object");
        }

        return request.getLocalPort();
    }

    /**
     * Return the underlying {@link WebappContext}
     *
     * @return Return the underlying {@link WebappContext}
     */
    protected WebappContext getContextImpl() {
        return contextImpl;
    }

    /**
     * Set the underlying {@link WebappContext}
     *
     * @param contextImpl the underlying {@link WebappContext}
     */
    protected void setContextImpl(WebappContext contextImpl) {
        this.contextImpl = contextImpl;
    }

    /**
     * Programmatically set the servlet path value. Default is an empty String.
     *
     * @param servletPath Servlet path to set.
     */
    public void setServletPath(final String servletPath) {
        if (servletPath != null) {
            if (servletPath.length() == 0) {
                this.servletPath = "";
            } else {
                this.servletPath = servletPath;
            }
        }
    }

    protected void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public Request getRequest() {
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletContext getServletContext() {
        return contextImpl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Request getInternalRequest() {
        return request;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DispatcherType getDispatcherType() {
        DispatcherType dispatcher = (DispatcherType) getAttribute(Globals.DISPATCHER_TYPE_ATTR);
        if (dispatcher == null) {
            dispatcher = DispatcherType.REQUEST;
        }
        return dispatcher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return startAsync(this, servletResponse);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext startAsync(final ServletRequest servletRequest, final ServletResponse servletResponse) throws IllegalStateException {
        return startAsync(servletRequest, servletResponse, false);
    }

    /**
     * Starts async processing on this request.
     *
     * @param servletRequest the ServletRequest with which to initialize the AsyncContext
     * @param servletResponse the ServletResponse with which to initialize the AsyncContext
     * @param isStartAsyncWithZeroArg true if the zero-arg version of startAsync was called, false otherwise
     */
    private AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse, boolean isStartAsyncWithZeroArg)
            throws IllegalStateException {

        if (servletRequest == null || servletResponse == null) {
            throw new IllegalArgumentException("Null request or response");
        }

        if (!isAsyncSupported()) {
            throw new IllegalStateException("Request is within the scope of a " + "filter or servlet that does not support asynchronous operations");
        }

        final AsyncContextImpl asyncContextLocal = asyncContext;

        if (asyncContextLocal != null) {
            if (isAsyncStarted()) {
                throw new IllegalStateException("ServletRequest.startAsync called" + " again without any asynchronous dispatch, or called "
                        + "outside the scope of any such dispatch, or called " + "again within the scope of the same dispatch");
            }
            if (isAsyncComplete) {
                throw new IllegalStateException("Response already closed");
            }
            if (!asyncContextLocal.isStartAsyncInScope()) {
                throw new IllegalStateException("ServletRequest.startAsync called " + "outside the scope of an async dispatch");
            }

            // Reinitialize existing AsyncContext
            asyncContextLocal.reinitialize(servletRequest, servletResponse, isStartAsyncWithZeroArg);
        } else {
            final AsyncContextImpl asyncContextFinal = new AsyncContextImpl(this, servletRequest, servletResponse, isStartAsyncWithZeroArg);
            asyncContext = asyncContextFinal;

            final CompletionHandler<org.glassfish.grizzly.http.server.Response> requestCompletionHandler = new EmptyCompletionHandler<org.glassfish.grizzly.http.server.Response>() {

                @Override
                public void completed(org.glassfish.grizzly.http.server.Response response) {
                    asyncContextFinal.notifyAsyncListeners(AsyncContextImpl.AsyncEventType.COMPLETE, null);
                }
            };

            final TimeoutHandler timeoutHandler = new TimeoutHandler() {

                @Override
                public boolean onTimeout(final org.glassfish.grizzly.http.server.Response response) {
                    return processTimeout();
                }
            };
            request.getResponse().suspend(-1, MILLISECONDS, requestCompletionHandler, timeoutHandler);
            asyncStartedThread = Thread.currentThread();
        }

        getInternalRequest().getContext().suspend();
        asyncStarted.set(true);

        return asyncContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncStarted() {
        return asyncStarted.get();
    }

    void setAsyncStarted(boolean asyncStarted) {
        this.asyncStarted.set(asyncStarted);
    }

    /**
     * Disables async support for this request.
     *
     * Async support is disabled as soon as this request has passed a filter or servlet that does not support async (either
     * via the designated annotation or declaratively).
     */
    public void disableAsyncSupport() {
        isAsyncSupported = false;
    }

    void setAsyncTimeout(long timeout) {
        request.getResponse().getSuspendContext().setTimeout(timeout, MILLISECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncSupported() {
        return isAsyncSupported;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AsyncContext getAsyncContext() {
        if (!isAsyncStarted()) {
            throw new IllegalStateException("The request has not been put into asynchronous mode, must call ServletRequest.startAsync first");
        }

        return asyncContext;
    }

    /*
     * Invokes any registered AsyncListener instances at their <tt>onComplete</tt> method
     */
    void asyncComplete() {
        if (isAsyncComplete) {
            throw new IllegalStateException("Request already released from asynchronous mode");
        }
        isAsyncComplete = true;
        asyncStarted.set(false);

        if (asyncStartedThread != Thread.currentThread() || !asyncContext.isOkToConfigure()) {
            // it's not safe to just mark response as resumed
            request.getResponse().resume();
        } else {
            final Response.SuspendedContextImpl suspendContext = (Response.SuspendedContextImpl) request.getResponse().getSuspendContext();

            suspendContext.markResumed();
            suspendContext.getSuspendStatus().reset();
        }
    }

    /*
     * Invokes all registered AsyncListener instances at their <tt>onTimeout</tt> method.
     *
     * This method also performs an error dispatch and completes the response if none of the listeners have done so.
     */
    void asyncTimeout() {
        if (asyncContext != null) {
            asyncContext.notifyAsyncListeners(AsyncContextImpl.AsyncEventType.TIMEOUT, null);
        }
        errorDispatchAndComplete(null);
    }

    /**
     * Notifies this Request that the container-initiated dispatch during which ServletRequest#startAsync was called is
     * about to return to the container
     */
    void onAfterService() throws IOException {
        if (asyncContext != null) {
            asyncContext.setOkToConfigure(false);

            if (asyncStarted.get()) {
                request.getResponse().getSuspendContext().setTimeout(asyncContext.getTimeout(), MILLISECONDS);
            }

        } else if (isUpgrade()) {
            if (httpUpgradeHandler != null) {
                final WebConnection wc = WebConnectionFactory.create(this, getInputStream(), servletResponse.getOutputStream());
                httpUpgradeHandler.init(wc);
            } else {
                LOGGER.log(Level.SEVERE, "HttpUpgradeHandler handler cannot be null");
            }
        }
    }

    private boolean processTimeout() {
        boolean result;
        final AsyncContextImpl asyncContextLocal = this.asyncContext;
        try {
            asyncTimeout();
        } finally {
            result = asyncContextLocal != null && !asyncContextLocal.getAndResetDispatchInScope();
        }

        return result;
    }

    void errorDispatchAndComplete(Throwable t) {
        /*
         * If no listeners, or none of the listeners called AsyncContext#complete or any of the AsyncContext#dispatch methods
         * (in which case asyncStarted would have been set to false), perform an error dispatch with a status code equal to 500.
         */
        if (asyncContext != null && !asyncContext.isDispatchInScope() && !isAsyncComplete && isAsyncStarted()) {
            servletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            response.setError();
            if (t != null) {
                setAttribute(RequestDispatcher.ERROR_EXCEPTION, t);
            }
            try {
//                if (hostValve != null) {
//                    hostValve.postInvoke(this, response);
//                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unable to perform error dispatch", e);
            } finally {
                /*
                 * If no matching error page was found, or the error page did not call AsyncContext#complete or any of the
                 * AsyncContext#dispatch methods, call AsyncContext#complete
                 */
                if (!isAsyncComplete && isAsyncStarted()) {
                    asyncComplete();
                }
            }
        }
    }

    /**
     * Create an instance of <code>HttpUpgradeHandler</code> for an given class and uses it for the http protocol upgrade
     * processing.
     *
     * @param handlerClass The <code>ProtocolHandler</code> class used for the upgrade.
     *
     * @return an instance of the <code>HttpUpgradeHandler</code>
     *
     * @exception IOException if an I/O error occurred during the upgrade
     *
     * @see jakarta.servlet.http.HttpUpgradeHandler
     * @see jakarta.servlet.http.WebConnection
     *
     * @since Servlet 3.1
     */
    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException {
        upgrade = true;
        T handler;
        try {
            handler = contextImpl.createHttpUpgradeHandlerInstance(handlerClass);
        } catch (Exception ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            } else {
                throw new IOException(ex);
            }
        }
        httpUpgradeHandler = handler;
        request.getResponse().suspend();
        return handler;
    }

    public boolean isUpgrade() {
        return upgrade;
    }

    public HttpUpgradeHandler getHttpUpgradeHandler() {
        return httpUpgradeHandler;
    }

    @Override
    public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void login(String string, String string1) throws ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Part getPart(String string) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpServletMapping getHttpServletMapping() {
        if (httpServletMapping == null) {
            httpServletMapping = new Mapping(request);
        }
        return httpServletMapping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PushBuilder newPushBuilder() {
        if (request.isPushEnabled()) {
            final org.glassfish.grizzly.http.server.http2.PushBuilder pushBuilder = request.newPushBuilder();
            return new PushBuilder() {

                @Override
                public PushBuilder method(final String method) {
                    pushBuilder.method(method);
                    return this;
                }

                @Override
                public PushBuilder queryString(final String queryString) {
                    pushBuilder.queryString(queryString);
                    return this;
                }

                @Override
                public PushBuilder sessionId(final String sessionId) {
                    pushBuilder.sessionId(sessionId);
                    return this;
                }

                @Override
                public PushBuilder setHeader(final String name, final String value) {
                    pushBuilder.setHeader(name, value);
                    return this;
                }

                @Override
                public PushBuilder addHeader(final String name, final String value) {
                    pushBuilder.addHeader(name, value);
                    return this;
                }

                @Override
                public PushBuilder removeHeader(final String name) {
                    pushBuilder.removeHeader(name);
                    return this;
                }

                @Override
                public PushBuilder path(final String path) {
                    pushBuilder.path(path);
                    return this;
                }

                @Override
                public void push() {
                    pushBuilder.push();
                }

                @Override
                public String getMethod() {
                    return pushBuilder.getMethod();
                }

                @Override
                public String getQueryString() {
                    return pushBuilder.getQueryString();
                }

                @Override
                public String getSessionId() {
                    return pushBuilder.getSessionId();
                }

                @Override
                public Set<String> getHeaderNames() {
                    final Iterable<String> i = pushBuilder.getHeaderNames();
                    final Set<String> names = new HashSet<>();
                    for (String s : i) {
                        names.add(s);
                    }

                    return names;
                }

                @Override
                public String getHeader(final String name) {
                    return pushBuilder.getHeader(name);
                }

                @Override
                public String getPath() {
                    return pushBuilder.getPath();
                }
            };
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getTrailerFields() {
        return request.getTrailers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTrailerFieldsReady() {
        return request.areTrailersAvailable();
    }

    @Override
    public String getRequestId() {
        return this.request.getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return this.request.getProtocolRequestId();
    }

    @Override
    public ServletConnection getServletConnection() {
        String connectionId = request.getConnection() == null ? null : Long.toString(request.getConnection().getId());
        return new ServletConnectionImpl(connectionId, request.getProtocol(), isSecure());
    }

    // ----------------------------------------------------------- DoPrivileged

    private final class GetAttributePrivilegedAction implements PrivilegedAction<Enumeration<String>> {

        @Override
        public Enumeration<String> run() {
            return new Enumerator<>(request.getAttributeNames());
        }
    }

    private final class GetParameterMapPrivilegedAction implements PrivilegedAction<Map<String, String[]>> {

        @Override
        public Map<String, String[]> run() {
            return request.getParameterMap();
        }
    }

    private final class GetRequestDispatcherPrivilegedAction implements PrivilegedAction<RequestDispatcher> {

        private final String path;

        public GetRequestDispatcherPrivilegedAction(String path) {
            this.path = path;
        }

        @Override
        public RequestDispatcher run() {
            return getRequestDispatcherInternal(path);
        }
    }

    private final class GetParameterPrivilegedAction implements PrivilegedAction<String> {

        public final String name;

        public GetParameterPrivilegedAction(String name) {
            this.name = name;
        }

        @Override
        public String run() {
            return request.getParameter(name);
        }
    }

    private final class GetParameterNamesPrivilegedAction implements PrivilegedAction<Set<String>> {

        @Override
        public Set<String> run() {
            return request.getParameterNames();
        }
    }

    private final class GetParameterValuePrivilegedAction implements PrivilegedAction<String[]> {

        public final String name;

        public GetParameterValuePrivilegedAction(String name) {
            this.name = name;
        }

        @Override
        public String[] run() {
            return request.getParameterValues(name);
        }
    }

    private final class GetCookiesPrivilegedAction implements PrivilegedAction<Cookie[]> {

        @Override
        public Cookie[] run() {
            return request.getCookies();
        }
    }

    private final class GetCharacterEncodingPrivilegedAction implements PrivilegedAction<String> {

        @Override
        public String run() {
            return request.getCharacterEncoding();
        }
    }

    private final class GetHeadersPrivilegedAction implements PrivilegedAction<Enumeration<String>> {

        private final String name;

        public GetHeadersPrivilegedAction(String name) {
            this.name = name;
        }

        @Override
        public Enumeration<String> run() {
            return new Enumerator<>(request.getHeaders(name));
        }
    }

    private final class GetHeaderNamesPrivilegedAction implements PrivilegedAction<Enumeration<String>> {

        @Override
        public Enumeration<String> run() {
            return new Enumerator<>(request.getHeaderNames());
        }
    }

    private final class GetLocalePrivilegedAction implements PrivilegedAction<Locale> {

        @Override
        public Locale run() {
            return request.getLocale();
        }
    }

    private final class GetLocalesPrivilegedAction implements PrivilegedAction<Enumeration<Locale>> {

        @Override
        public Enumeration<Locale> run() {
            return new Enumerator<>(request.getLocales());
        }
    }


    /**
     * The static factory is required to avoid NoClassDefFoundError when running w/ Servlet 3.0 API and JDK6
     */
    static class WebConnectionFactory {
        static WebConnection create(final HttpServletRequestImpl req, final ServletInputStream inputStream, final ServletOutputStream outputStream) {
            return new WebConnectionImpl(req, inputStream, outputStream);
        }
    }

    private static final class Mapping implements HttpServletMapping {

        private String matchValue;
        private final String pattern;
        private final String servletName;
        private MappingMatch mappingMatch;

        private Mapping(final Request request) {
            final MappingData data = request.obtainMappingData();
            if (data == null) {
                throw new NullPointerException("No MappingData available.");
            }

            // Trim leading "/"
            matchValue = data.matchedPath != null && data.matchedPath.length() >= 2 ? data.matchedPath.substring(1) : "";
            pattern = data.descriptorPath != null ? data.descriptorPath : "";
            servletName = data.servletName != null ? data.servletName : "";

            switch (data.mappingType) {
            case MappingData.CONTEXT_ROOT:
                mappingMatch = MappingMatch.CONTEXT_ROOT;
                break;
            case MappingData.DEFAULT:
                mappingMatch = MappingMatch.DEFAULT;
                break;
            case MappingData.EXACT:
                mappingMatch = MappingMatch.EXACT;
                break;
            case MappingData.EXTENSION:
                mappingMatch = MappingMatch.EXTENSION;
                // Ensure pattern is valid
                if (pattern.charAt(0) == '*') {
                    // Mutate matchValue to mean "what * was matched with".
                    int i = matchValue.indexOf(pattern.substring(1));
                    if (-1 != i) {
                        matchValue = matchValue.substring(0, i);
                    }
                }
                break;
            case MappingData.PATH:
                mappingMatch = MappingMatch.PATH;
                // Ensure pattern is valid
                int patternLen = pattern.length();
                if (patternLen > 0 && pattern.charAt(patternLen - 1) == '*') {
                    int indexOfPatternStart = patternLen - 2;
                    int matchValueLen = matchValue.length();
                    if (0 <= indexOfPatternStart && indexOfPatternStart < matchValueLen) {
                        // Remove the pattern from the end of matchValue
                        matchValue = matchValue.substring(indexOfPatternStart);
                    }
                }
                break;
            }
        }

        @Override
        public String getMatchValue() {
            return matchValue;
        }

        @Override
        public String getPattern() {
            return pattern;
        }

        @Override
        public String getServletName() {
            return servletName;
        }

        @Override
        public MappingMatch getMappingMatch() {
            return mappingMatch;
        }

        @Override
        public String toString() {
            return "Mapping{" + "matchValue='" + matchValue + '\'' + ", pattern='" + pattern + '\'' + ", servletName='" + servletName + '\'' + ", mappingMatch="
                    + mappingMatch + '}';
        }
    }
}
