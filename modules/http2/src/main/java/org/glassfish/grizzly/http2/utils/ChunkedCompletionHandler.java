/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2.utils;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.WriteResult;

/**
 *
 * @author oleksiys
 */
public class ChunkedCompletionHandler implements CompletionHandler<WriteResult> {
    private final CompletionHandler<WriteResult> parentCompletionHandler;

    private boolean isDone;
    protected int chunksCounter = 1;
    private long writtenSize;

    public ChunkedCompletionHandler(
            final CompletionHandler<WriteResult> parentCompletionHandler) {
        this.parentCompletionHandler = parentCompletionHandler;
    }

    public void incChunks() {
        chunksCounter++;
    }
    
    @Override
    public void cancelled() {
        if (done()) {
            if (parentCompletionHandler != null) {
                parentCompletionHandler.cancelled();
            }
        }
    }

    @Override
    public void failed(Throwable throwable) {
        if (done()) {
            if (parentCompletionHandler != null) {
                parentCompletionHandler.failed(throwable);
            }
        }
    }

    @Override
    public void completed(final WriteResult result) {
        if (isDone) {
            return;
        }

        if (--chunksCounter == 0) {
            done();

            final long initialWrittenSize = result.getWrittenSize();
            writtenSize += initialWrittenSize;

            if (parentCompletionHandler != null) {
                try {
                    result.setWrittenSize(writtenSize);
                    parentCompletionHandler.completed(result);
                } finally {
                    result.setWrittenSize(initialWrittenSize);
                }
            }
        } else {
            updated(result);
            writtenSize += result.getWrittenSize();
        }
    }

    @Override
    public void updated(final WriteResult result) {
        if (parentCompletionHandler != null) {
            final long initialWrittenSize = result.getWrittenSize();

            try {
                result.setWrittenSize(writtenSize + initialWrittenSize);
                parentCompletionHandler.updated(result);
            } finally {
                result.setWrittenSize(initialWrittenSize);
            }
        }
    }

    private boolean done() {
        if (isDone) {
            return false;
        }

        isDone = true;
        
        done0();
        return true;
    }
    
    protected void done0() {
    }
}
