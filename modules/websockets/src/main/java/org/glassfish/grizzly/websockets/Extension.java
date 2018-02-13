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

package org.glassfish.grizzly.websockets;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a WebSocket extension and its associated parameters.
 *
 * @since 2.3
 */
public final class Extension {

    private final String name;
    private final List<Parameter> parameters = new ArrayList<Parameter>();


    // ------------------------------------------------------------ Constructors


    /**
     * Constructs a new Extension with the specified name.
     *
     * @param name extension name
     */
    public Extension(final String name) {
        this.name = name;
    }


    // ---------------------------------------------------------- Public Methods


    /**
     * @return the extension name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return any parameters associated with this extension.
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Extension extension = (Extension) o;

        return name.equals(extension.name)
                && parameters.equals(extension.parameters);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (!parameters.isEmpty()) {
            for (Extension.Parameter p : parameters) {
                sb.append("; ");
                sb.append(p.toString());
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------- Nested Classes


    /**
     * Representation of extension parameters.
     */
    public static final class Parameter {

        private final String name;
        private String value;


        // -------------------------------------------------------- Constructors


        /**
         * Constructs a new parameter based on the provided values.
         *
         * @param name the name of the parameter (may not be <code>null</code>).
         * @param value the value of the parameter (may be <code>null</code>).
         *
         * @throws IllegalArgumentException if name is <code>null</code>.
         */
        public Parameter(final String name, final String value) {
            if (name == null) {
                throw new IllegalArgumentException("Parameter name may not be null");
            }
            this.name = name;
            this.value = value;
        }


        // ------------------------------------------------------ Public Methods

        /**
         * @return the parameter name.
         */
        public String getName() {
            return name;
        }

        /**
         * @return the parameter value; may be <code>null</code>.
         */
        public String getValue() {
            return value;
        }

        /**
         * Set the value of this parameter.
         *
         * @param value the value of this parameter.
         */
        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Parameter parameter = (Parameter) o;

            return name.equals(parameter.name)
                    && !(value != null
                             ? !value.equals(parameter.value)
                             : parameter.value != null);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + (value != null ? value.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(name);
            if (value != null) {
                sb.append('=').append(value);
            }
            return sb.toString();
        }
    } // END Parameter

}
