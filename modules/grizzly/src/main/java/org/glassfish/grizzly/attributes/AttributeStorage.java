/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.attributes;

/**
 * <tt>AttributeStorage</tt> provides access to a {@link AttributeHolder}, where application can store
 * {@link Attribute}s. This interface lets us describe class, which is not {@link AttributeHolder} itself, but has
 * associated {@link AttributeHolder}.
 *
 * @see AttributeHolder
 *
 * @author Alexey Stashok
 */
public interface AttributeStorage {
    /**
     * Get associated {@link AttributeHolder}. Implementation may return <tt>null</tt> if {@link AttributeHolder} wasn't
     * initialized yet.
     *
     * @return associated {@link AttributeHolder}. Implementation may return <tt>null</tt> if {@link AttributeHolder} wasn't
     * initialized yet.
     */
    AttributeHolder getAttributes();
}
