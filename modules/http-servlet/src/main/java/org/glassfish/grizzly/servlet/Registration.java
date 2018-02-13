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

package org.glassfish.grizzly.servlet;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class for all complex registrable components within a web application.
 *
 * @since 2.2
 */
public abstract class Registration {

    protected String name;
    protected String className;
    protected Map<String,String> initParameters;
    protected final WebappContext ctx;

    // ------------------------------------------------------------ Constructors


    protected Registration(final WebappContext ctx,
                           final String name,
                           final String className) {
        this.ctx = ctx;
        this.name = name;
        this.className = className;
        initParameters = new LinkedHashMap<String, String>(4, 1.0f);
    }

    // ---------------------------------------------------------- Public Methods

    /**
     * Gets the name of the Servlet or Filter that is represented by this
     * Registration.
     *
     * @return the name of the Servlet or Filter that is represented by this
     * Registration
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the fully qualified class name of the Servlet or Filter that
     * is represented by this Registration.
     *
     * @return the fully qualified class name of the Servlet or Filter
     * that is represented by this Registration, or null if this
     * Registration is preliminary
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the initialization parameter with the given name and value
     * on the Servlet or Filter that is represented by this Registration.
     *
     * @param name the initialization parameter name
     * @param value the initialization parameter value
     *
     * @return true if the update was successful, i.e., an initialization
     * parameter with the given name did not already exist for the Servlet
     * or Filter represented by this Registration, and false otherwise
     *
     * @throws IllegalStateException if the WebappContext from which this
     * Registration was obtained has already been initialized
     * @throws IllegalArgumentException if the given name or value is
     * <tt>null</tt>
     */
    public boolean setInitParameter(String name, String value) {
        if (ctx.deployed) {
            throw new IllegalStateException("WebappContext has already been deployed");
        }
        if (name == null) {
            throw new IllegalArgumentException("'name' cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("'value' cannot be null");
        }
        if (!initParameters.containsKey(name)) {
            initParameters.put(name, value);
            return true;
        }
        return false;
    }

    /**
     * Gets the value of the initialization parameter with the given name
     * that will be used to initialize the Servlet or Filter represented
     * by this Registration object.
     *
     * @param name the name of the initialization parameter whose value is
     * requested
     *
     * @return the value of the initialization parameter with the given
     * name, or <tt>null</tt> if no initialization parameter with the given
     * name exists
     */
    public String getInitParameter(String name) {
        return ((name == null) ? null : initParameters.get(name));
    }

    /**
     * Sets the given initialization parameters on the Servlet or Filter
     * that is represented by this Registration.
     *
     * <p>The given map of initialization parameters is processed
     * <i>by-value</i>, i.e., for each initialization parameter contained
     * in the map, this method calls {@link #setInitParameter(String,String)}.
     * If that method would return false for any of the
     * initialization parameters in the given map, no updates will be
     * performed, and false will be returned. Likewise, if the map contains
     * an initialization parameter with a <tt>null</tt> name or value, no
     * updates will be performed, and an IllegalArgumentException will be
     * thrown.
     *
     * @param initParameters the initialization parameters
     *
     * @return the (possibly empty) Set of initialization parameter names
     * that are in conflict
     *
     * @throws IllegalStateException if the WebappContext from which this
     * Registration was obtained has already been initialized
     * @throws IllegalArgumentException if the given map contains an
     * initialization parameter with a <tt>null</tt> name or value
     */
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        if (ctx.deployed) {
            throw new IllegalStateException("WebappContext has already been deployed");
        }
        if (initParameters == null) {
            return Collections.emptySet();
        }
        final Set<String> conflicts = new LinkedHashSet<String>(4, 1.0f);
        for (final Map.Entry<String,String> entry : initParameters.entrySet()) {
            if (!setInitParameter(entry.getKey(), entry.getValue())) {
                conflicts.add(entry.getKey());
            }
        }
        return conflicts;
    }

    /**
     * Gets an immutable (and possibly empty) Map containing the
     * currently available initialization parameters that will be used to
     * initialize the Servlet or Filter represented by this Registration
     * object.
     *
     * @return Map containing the currently available initialization
     * parameters that will be used to initialize the Servlet or Filter
     * represented by this Registration object
     */
    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Registration)) return false;

        Registration that = (Registration) o;

        if (className != null ? !className.equals(that.className) : that.className != null)
            return false;
        if (ctx != null ? !ctx.equals(that.ctx) : that.ctx != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (ctx != null ? ctx.hashCode() : 0);
        return result;
    }
}
