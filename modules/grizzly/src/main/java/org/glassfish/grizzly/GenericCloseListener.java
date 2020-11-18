/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

/**
 * This interface was added to 2.3 to avoid having to declare {@link CloseListener} with parameter types. This interface
 * will not be present in 3.0. In 3.0, it will be required that all close listeners implement {@link CloseListener}
 * without generic arguments.
 *
 * @since 2.3
 *
 * @deprecated
 */
@Deprecated
public interface GenericCloseListener extends CloseListener<Closeable, CloseType> {

}
