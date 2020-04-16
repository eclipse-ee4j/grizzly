/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.servlet;

import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionIdListener;
import jakarta.servlet.http.HttpSessionListener;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.server.Session;
import org.glassfish.grizzly.localization.LogMessages;

import static java.util.concurrent.TimeUnit.*;

/**
 * Basic {@link HttpSession} based on {@link Session} support.
 * 
 * @author Jeanfrancois Arcand
 */
@SuppressWarnings("deprecation")
public class HttpSessionImpl implements HttpSession {

    private static final Logger LOGGER = Grizzly.logger(HttpSessionImpl.class);
    /**
     * The real session object
     */
    private final Session session;
    /**
     * The ServletContext.
     */
    private final WebappContext contextImpl;

    /**
     * Create an HttpSession.
     * @param contextImpl
     * @param session internal session object
     */
    public HttpSessionImpl(final WebappContext contextImpl,
            final Session session) {
        this.contextImpl = contextImpl;
        this.session = session;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getCreationTime() {
        if (!session.isValid()) {
            throw new IllegalStateException("The session was invalidated");
        }
        
        return session.getCreationTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return session.getIdInternal();
    }

    /**
     * Is the current Session valid?
     * @return true if valid.
     */
    protected boolean isValid() {
        return session.isValid();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public long getLastAccessedTime() {
        if (!session.isValid()) {
            throw new IllegalStateException("The session was invalidated");
        }

        return session.getTimestamp();
    }

    /**
     * Reset the timestamp.
     */
    protected void access() {
        session.access();
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
    public void setMaxInactiveInterval(int sessionTimeout) {
        if (sessionTimeout < 0) {
            sessionTimeout = -1;
        } else {
            sessionTimeout = (int) MILLISECONDS.convert(sessionTimeout, SECONDS);
        }
        
        session.setSessionTimeout(sessionTimeout);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxInactiveInterval() {
        long sessionTimeout = session.getSessionTimeout();
        if (sessionTimeout < 0) {
            return -1;
        }

        sessionTimeout = SECONDS.convert(sessionTimeout, MILLISECONDS);
        if (sessionTimeout > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(sessionTimeout + " cannot be cast to int.");
        }
        
        return (int) sessionTimeout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public jakarta.servlet.http.HttpSessionContext getSessionContext() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(String key) {
        return session.getAttribute(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(String value) {
        return session.getAttribute(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(session.attributes().keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getValueNames() {
        return session.attributes().entrySet().toArray(
                new String[session.attributes().size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(String key, Object value) {
        
         // Null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(key);
            return;
        }
        
        Object unbound = session.getAttribute(key);
        session.setAttribute(key, value);

        // Call the valueUnbound() method if necessary
        if ((unbound != null) && (unbound != value) &&
                (unbound instanceof HttpSessionBindingListener)) {
            try {
                ((HttpSessionBindingListener) unbound).valueUnbound(new HttpSessionBindingEvent(this, key));
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING,
                               LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_SESSION_LISTENER_UNBOUND_ERROR(unbound.getClass().getName()));
                }
            }
        }
        // Construct an event with the new value
        HttpSessionBindingEvent event = null;

        // Call the valueBound() method if necessary
        if (value instanceof HttpSessionBindingListener) {
            if (value != unbound) {
                event = new HttpSessionBindingEvent(this, key, value);
                try {
                    ((HttpSessionBindingListener) value).valueBound(event);
                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING,
                                LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_SESSION_LISTENER_BOUND_ERROR(value.getClass().getName()));
                    }
                }
            }
        }

        // Notify interested application event listeners
        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length == 0) {
            return;
        }
        for (int i = 0, len = listeners.length; i < len; i++) {
            if (!(listeners[i] instanceof HttpSessionAttributeListener)) {
                continue;
            }
            HttpSessionAttributeListener listener =
                    (HttpSessionAttributeListener) listeners[i];
            try {
                if (unbound != null) {
                    if (event == null) {
                        event = new HttpSessionBindingEvent(this, key, unbound);
                    }
                    listener.attributeReplaced(event);
                } else {
                    if (event == null) {
                        event = new HttpSessionBindingEvent(this, key, value);
                    }
                    listener.attributeAdded(event);
                }
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING,
                               LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_ATTRIBUTE_LISTENER_ADD_ERROR("HttpSessionAttributeListener", listener.getClass().getName()),
                               t);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putValue(String key, Object value) {
        setAttribute(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAttribute(String key) {
        Object value = session.removeAttribute(key);
  
        if (value == null) {
            return;
        }

        // Call the valueUnbound() method if necessary
        HttpSessionBindingEvent event = null;
        if (value instanceof HttpSessionBindingListener) {
            event = new HttpSessionBindingEvent(this,key, value);
            ((HttpSessionBindingListener) value).valueUnbound(event);
        }

        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length == 0)
            return;
        for (int i = 0, len = listeners.length; i < len; i++) {
            if (!(listeners[i] instanceof HttpSessionAttributeListener))
                continue;
            HttpSessionAttributeListener listener =
                (HttpSessionAttributeListener) listeners[i];
            try {
                if (event == null) {
                    event = new HttpSessionBindingEvent
                        (this, key, value);
                }
                listener.attributeRemoved(event);
            } catch (Throwable t) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING,
                               LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_ATTRIBUTE_LISTENER_REMOVE_ERROR("HttpSessionAttributeListener", listener.getClass().getName()),
                               t);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeValue(String key) {
        removeAttribute(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void invalidate() {
        session.setValid(false);
        session.attributes().clear();

        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length > 0) {
            HttpSessionEvent event =
                    new HttpSessionEvent(this);
            for (int i = 0, len = listeners.length; i < len; i++) {
                Object listenerObj = listeners[i];
                if (!(listenerObj instanceof HttpSessionListener)) {
                    continue;
                }
                HttpSessionListener listener =
                        (HttpSessionListener) listenerObj;
                try {
                    listener.sessionDestroyed(event);
                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING,
                                   LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_CONTAINER_OBJECT_DESTROYED_ERROR("sessionDestroyed", "HttpSessionListener", listener.getClass().getName()),
                                   t);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNew() {
        if (!session.isValid()) {
            throw new IllegalStateException("The session was invalidated");
        }
        
        return session.isNew();
    }

    /**
     * Invoke to notify all registered {@link HttpSessionListener} of the 
     * session has just been created.
     */
    protected void notifyNew() {
        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length > 0) {
            HttpSessionEvent event =
                    new HttpSessionEvent(this);
            for (int i = 0, len = listeners.length; i < len; i++) {
                Object listenerObj = listeners[i];
                if (!(listenerObj instanceof HttpSessionListener)) {
                    continue;
                }
                HttpSessionListener listener =
                        (HttpSessionListener) listenerObj;
                try {
                    listener.sessionCreated(event);
                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING,
                                   LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_CONTAINER_OBJECT_INITIALIZED_ERROR("sessionCreated", "HttpSessionListener", listener.getClass().getName()),
                                   t);
                    }
                }
            }
        }
    }
    
    /**
     * Invoke to notify all registered {@link HttpSessionListener} of the 
     * session has just been created.
     */
    protected void notifyIdChanged(final String oldId) {
        EventListener[] listeners = contextImpl.getEventListeners();
        if (listeners.length > 0) {
            HttpSessionEvent event =
                    new HttpSessionEvent(this);
            for (int i = 0, len = listeners.length; i < len; i++) {
                Object listenerObj = listeners[i];
                if (!(listenerObj instanceof HttpSessionIdListener)) {
                    continue;
                }
                HttpSessionIdListener listener =
                        (HttpSessionIdListener) listenerObj;
                try {
                    listener.sessionIdChanged(event, oldId);
                } catch (Throwable t) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.log(Level.WARNING,
                                   LogMessages.WARNING_GRIZZLY_HTTP_SERVLET_CONTAINER_OBJECT_INITIALIZED_ERROR("sessionCreated", "HttpSessionListener", listener.getClass().getName()),
                                   t);
                    }
                }
            }
        }
    }    
}
