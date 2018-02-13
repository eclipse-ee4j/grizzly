/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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
 * {@link NextAction} implementation, which instructs {@link FilterChain} to
 * process next {@link Filter} in chain.
 *
 * So any {@link Filter} implementation is free to change the {@link Filter}
 * execution sequence.
 *
 * @see NextAction
 * 
 * @author Alexey Stashok
 */
final class InvokeAction extends AbstractNextAction {
    static final int TYPE = 0;

    private Appender appender;
    private Object chunk;
    
    private boolean isIncomplete;
    
    InvokeAction() {
        super(TYPE);
    }

    public Object getChunk() {
        return chunk;
    }

    public Appender getAppender() {
        return appender;
    }

    public boolean isIncomplete() {
        return isIncomplete;
    }

    public void setUnparsedChunk(final Object unparsedChunk) {
        chunk = unparsedChunk;
        appender = null;
        isIncomplete = false;
    }
    
    public <E> void setIncompleteChunk(final E incompleteChunk,
            final Appender<E> appender) {
        chunk = incompleteChunk;
        this.appender = appender;
        isIncomplete = true;
    }
    
    void reset() {
        isIncomplete = false;
        chunk = null;
        appender = null;
    }
}
