/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple session object.
 *
 * @author Jeanfrancois Arcand
 */
public class Session {

    /**
     * Cache attribute (thread safe)
     */
    private final ConcurrentMap<String,Object> attributes =
            new ConcurrentHashMap<>();

    /**
     * A session identifier
     */
    private String id = null;


    /**
     * Is this Session valid.
     */
    private boolean isValid = true;

    /**
     * Is this session new.
     */
    private boolean isNew = true;

    /**
     * When this session was created.
     */
    private final long creationTime;

    /**
     * Timeout
     */
    private long sessionTimeout = -1;


     /**
     * Creation time stamp.
     */
    private long timestamp = -1;



    public Session() {
        this(null);
    }


    /**
     * Create a new session using a session identifier
     * @param id session identifier
     */
    public Session(String id) {
        this.id = id;
        creationTime = timestamp = System.currentTimeMillis();
    }


    /**
     * Is the current Session valid?
     * @return true if valid.
     */
    public boolean isValid() {
        return isValid;
    }


    /**
     * Set this object as validated.
     * @param isValid
     */
    public void setValid(boolean isValid) {
        this.isValid = isValid;
        if (!isValid) {
            timestamp = -1;
        }
    }

    /**
     * Returns <code>true</code> if the client does not yet know about the
     * session or if the client chooses not to join the session.  For 
     * example, if the server used only cookie-based sessions, and
     * the client had disabled the use of cookies, then a session would
     * be new on each request.
     *
     * @return 				<code>true</code> if the 
     *					server has created a session, 
     *					but the client has not yet joined
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * @return the session identifier for this session.
     */
    public String getIdInternal() {
        return id;
    }


    /**
     * Sets the session identifier for this session.
     * @param id
     */
    protected void setIdInternal(String id) {
        this.id = id;
    }


    /**
     * Add an attribute to this session.
     * @param key
     * @param value
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Return an attribute.
     *
     * @param key
     * @return an attribute
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }


    /**
     * Remove an attribute.
     * @param key
     * @return true if successful.
     */
    public Object removeAttribute(String key){
        return attributes.remove(key);
    }


    /**
     * Return a {@link ConcurrentMap} of attributes.
     * @return the attributes associated with this session.
     */
    public ConcurrentMap<String,Object> attributes() {
        return attributes;
    }

    /**
     *
     * Returns the time when this session was created, measured
     * in milliseconds since midnight January 1, 1970 GMT.
     *
     * @return				a <code>long</code> specifying
     * 					when this session was created,
     *					expressed in 
     *					milliseconds since 1/1/1970 GMT
     */
    public long getCreationTime() {
        return creationTime;
    }
    
    /**
     * Return a long representing the maximum idle time (in milliseconds) a session can be.
     * @return a long representing the maximum idle time (in milliseconds) a session can be.
     */
    public long getSessionTimeout() {
        return sessionTimeout;
    }


    /**
     * Set a long representing the maximum idle time (in milliseconds) a session can be.
     * @param sessionTimeout a long representing the maximum idle time (in milliseconds) a session can be.
     */
    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }


    /**
     * @return the timestamp when this session was accessed the last time
     */
    public long getTimestamp() {
        return timestamp;
    }


    /**
     * Set the timestamp when this session was accessed the last time.
     * @param timestamp a long representing when the session was accessed the last time
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Updates the "last accessed" timestamp with the current time.
     * @return the time stamp
     */
    public long access() {
        final long localTimeStamp = System.currentTimeMillis();
        timestamp = localTimeStamp;
        isNew = false;
        
        return localTimeStamp;
    }
}
