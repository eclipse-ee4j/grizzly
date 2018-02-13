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

package org.glassfish.grizzly.asyncqueue;

import org.glassfish.grizzly.Connection;

/**
 * Callback handler, which will be called by Grizzly {@link org.glassfish.grizzly.Writer}
 * implementation, if message can not be neither written nor added to write queue
 * at the moment due to I/O or memory limitations.
 * User may perform one of the actions proposed by {@link PushBackContext} or
 * implement any other custom processing logic.
 * 
 * @since 2.2
 * 
 * @deprecated push back logic is deprecated.
 * 
 * @author Alexey Stashok
 */
@SuppressWarnings("deprecation")
public interface PushBackHandler {

    /**
     * The method is invoked once message is accepted by
     * {@link org.glassfish.grizzly.Writer}. It means either message was written
     * or scheduled to be written asynchronously.
     * 
     * @param connection {@link Connection}
     * @param message {@link WritableMessage}
     */
    void onAccept(Connection connection, WritableMessage message);

    /**
     * The method is invoked if message was refused by {@link org.glassfish.grizzly.Writer}
     * due to I/O or memory limitations.
     * At this point user can perform one of the actions proposed by {@link PushBackContext},
     * or implement any custom processing logic.
     * 
     * @param connection {@link Connection}
     * @param message {@link WritableMessage}
     * @param pushBackContext {@link PushBackContext}
     */
    void onPushBack(Connection connection, WritableMessage message,
                    PushBackContext pushBackContext);
    
}
