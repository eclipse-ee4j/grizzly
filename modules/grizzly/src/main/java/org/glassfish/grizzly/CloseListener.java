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

import java.io.IOException;

/**
 * The listener, which is used to be notified, when <tt>Connection</tt> gets closed. While this interface is public,
 * it's not intended for public consumption at this time. This interface will be changing in 3.0.
 */
public interface CloseListener<T extends Closeable, C extends ICloseType> {
    void onClosed(T closeable, C type) throws IOException;
}
