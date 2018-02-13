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

package org.glassfish.grizzly;

import java.io.IOException;

/**
 * Thrown when an attempt is made to add a record that exceeds
 * the configured maximum queue size.
 *
 * @since 2.0
 */
public final class PendingWriteQueueLimitExceededException extends IOException {
    private static final long serialVersionUID = -7713985866708297095L;

    public PendingWriteQueueLimitExceededException() {
        super();
    }

    public PendingWriteQueueLimitExceededException(String message) {
        super(message);
    }
}
