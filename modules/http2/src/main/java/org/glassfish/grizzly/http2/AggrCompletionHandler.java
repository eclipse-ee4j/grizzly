/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import java.util.Arrays;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.WriteResult;

/**
 *
 * @author oleksiys
 */
class AggrCompletionHandler implements CompletionHandler<WriteResult> {
    private Record[] completionHandlerRecords = new Record[2];
    private int recordsCount;

    public void register(final CompletionHandler<WriteResult> completionHandler, final int bytesWrittenToReport) {
        ensureCapacity();
        Record record = completionHandlerRecords[recordsCount];
        if (record == null) {
            record = new Record(completionHandler, bytesWrittenToReport);
            completionHandlerRecords[recordsCount] = record;
        } else {
            record.set(completionHandler, bytesWrittenToReport);
        }

        recordsCount++;
    }

    @Override
    public void cancelled() {
        for (int i = 0; i < recordsCount; i++) {
            try {
                final Record record = completionHandlerRecords[i];
                final CompletionHandler<WriteResult> completionHandler = record.completionHandler;
                record.reset();

                completionHandler.cancelled();
            } catch (Exception ignored) {
            }
        }

        recordsCount = 0;
    }

    @Override
    public void failed(final Throwable throwable) {
        for (int i = 0; i < recordsCount; i++) {
            try {
                final Record record = completionHandlerRecords[i];
                final CompletionHandler<WriteResult> completionHandler = record.completionHandler;
                record.reset();

                completionHandler.failed(throwable);
            } catch (Exception ignored) {
            }
        }

        recordsCount = 0;
    }

    @Override
    public void completed(final WriteResult result) {
        final long originalWrittenSize = result.getWrittenSize();

        for (int i = 0; i < recordsCount; i++) {
            try {
                final Record record = completionHandlerRecords[i];
                final CompletionHandler<WriteResult> completionHandler = record.completionHandler;
                final int bytesWrittenToReport = record.bytesWrittenToReport;

                record.reset();

                result.setWrittenSize(bytesWrittenToReport);
                completionHandler.completed(result);
            } catch (Exception ignored) {
            }
        }

        result.setWrittenSize(originalWrittenSize);
        recordsCount = 0;
    }

    @Override
    public void updated(WriteResult result) {
        // don't call update on internal CompletionHandlers
    }

    private void ensureCapacity() {
        if (completionHandlerRecords.length == recordsCount) {
            completionHandlerRecords = Arrays.copyOf(completionHandlerRecords, recordsCount + (recordsCount >> 1) + 1);
        }
    }

    private static class Record {

        private CompletionHandler<WriteResult> completionHandler;
        private int bytesWrittenToReport;

        Record(final CompletionHandler<WriteResult> completionHandler, final int bytesWrittenToReport) {
            this.completionHandler = completionHandler;
            this.bytesWrittenToReport = bytesWrittenToReport;
        }

        void set(final CompletionHandler<WriteResult> completionHandler, final int bytesWrittenToReport) {
            this.completionHandler = completionHandler;
            this.bytesWrittenToReport = bytesWrittenToReport;
        }

        void reset() {
            this.completionHandler = null;
        }
    }
}
