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

package org.glassfish.grizzly.filterchain;

import org.glassfish.grizzly.Appender;

/**
 * {@link NextAction}, which instructs {@link FilterChain} to stop executing phase and start post executing filters.
 *
 * @author Alexey Stashok
 */
final class StopAction extends AbstractNextAction {
    static final int TYPE = 1;

    private Appender appender;
    private Object incompleteChunk;

    StopAction() {
        super(TYPE);
    }

    public Object getIncompleteChunk() {
        return incompleteChunk;
    }

    public Appender getAppender() {
        return appender;
    }

    public <E> void setIncompleteChunk(E incompleteChunk, Appender<E> appender) {
        this.incompleteChunk = incompleteChunk;
        this.appender = appender;
    }

    void reset() {
        incompleteChunk = null;
        appender = null;
    }
}
