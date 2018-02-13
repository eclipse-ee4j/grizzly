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

package org.glassfish.grizzly.streams;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.utils.conditions.Condition;
import java.io.IOException;

/**
 *
 * @author Alexey Stashok
 */
public interface Input {
    GrizzlyFuture<Integer> notifyCondition(
            final Condition condition,
            final CompletionHandler<Integer> completionHandler);

    byte read() throws IOException;

    void skip(int length);
    
    boolean isBuffered();
    
    /**
     * Return the <tt>Input</tt>'s {@link Buffer}.
     *
     * @return the <tt>Input</tt>'s {@link Buffer}.
     */
    Buffer getBuffer();

    /**
     * Takes the <tt>Input</tt>'s {@link Buffer}. This <tt>Input</tt> should
     * never try to access this {@link Buffer}.
     *
     * @return the <tt>Input</tt>'s {@link Buffer}. This <tt>Input</tt> should
     * never try to access this {@link Buffer}.
     */
    Buffer takeBuffer();

    int size();

    void close() throws IOException;
}
