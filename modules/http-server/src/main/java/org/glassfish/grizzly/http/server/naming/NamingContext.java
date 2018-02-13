/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server.naming;

/**
 * This interface represents a naming context, which
 * consists of a set of name-to-object bindings.
 * 
 * @see javax.naming.Context
 * 
 * @author Grizzly Team
 */
public interface NamingContext {

    /**
     * Retrieves the named object.
     * See {@link #lookup(Name)} for details.
     * @param name
     *          the name of the object to look up
     * @return  the object bound to <tt>name</tt>
     * @throws  NamingException if a naming exception is encountered
     */
    Object lookup(String pathStr) throws NamingException;
    
}
