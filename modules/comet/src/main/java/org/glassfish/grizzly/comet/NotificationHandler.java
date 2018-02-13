/*
 * Copyright (c) 2007, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.comet;

import java.io.IOException;
import java.util.Iterator;

/**
 * This class is invoked when the CometContext.notify is invoked. The CometContext delegate the handling of the
 * notification process to an implementation of this interface.
 *
 * @author Jeanfrancois Arcand
 */
public interface NotificationHandler {
    /**
     * Notify all {@link CometHandler}.
     *
     * @param cometEvent the CometEvent used to notify CometHandler
     * @param iteratorHandlers An iterator over a list of CometHandler
     */
    void notify(CometEvent cometEvent, Iterator<CometHandler> iteratorHandlers) throws IOException;

    /**
     * Notify a single {@link CometHandler}.
     *
     * @param cometEvent the CometEvent used to notify CometHandler
     * @param cometHandler An iterator over a list of CometHandler
     */
    void notify(CometEvent cometEvent, CometHandler cometHandler) throws IOException;
}
