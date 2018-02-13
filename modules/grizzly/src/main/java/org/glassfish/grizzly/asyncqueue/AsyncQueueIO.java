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

package org.glassfish.grizzly.asyncqueue;

/**
 * Class encapsulates asynchronous queue implementation:
 * {@link AsyncQueueReader}, {@link AsyncQueueWriter}.
 * 
 * @author Alexey Stashok
 */
public interface AsyncQueueIO<L> {
    /**
     * Get {@link AsyncQueueReader} implementation.
     * 
     * @return {@link AsyncQueueReader} implementation.
     */
    AsyncQueueReader<L> getReader();

    /**
     * Get {@link AsyncQueueWriter} implementation.
     *
     * @return {@link AsyncQueueWriter} implementation.
     */
    AsyncQueueWriter<L> getWriter();
    
    class Factory {
        public static <L> AsyncQueueIO<L> createImmutable(
                final AsyncQueueReader<L> reader, final AsyncQueueWriter<L> writer) {
            return new ImmutableAsyncQueueIO<L>(reader, writer);
        }
        
        public static <L> MutableAsyncQueueIO<L> createMutable(
                final AsyncQueueReader<L> reader, final AsyncQueueWriter<L> writer) {
            return new MutableAsyncQueueIO<L>(reader, writer);
        }
    }
    
    final class ImmutableAsyncQueueIO<L> implements AsyncQueueIO<L> {

        private final AsyncQueueReader<L> reader;
        private final AsyncQueueWriter<L> writer;

        private ImmutableAsyncQueueIO(final AsyncQueueReader<L> reader, final AsyncQueueWriter<L> writer) {
            this.reader = reader;
            this.writer = writer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncQueueReader<L> getReader() {
            return reader;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncQueueWriter<L> getWriter() {
            return writer;
        }
    }
    
    final class MutableAsyncQueueIO<L> implements AsyncQueueIO<L> {

        private volatile AsyncQueueReader<L> reader;
        private volatile AsyncQueueWriter<L> writer;

        private MutableAsyncQueueIO(final AsyncQueueReader<L> reader,
                final AsyncQueueWriter<L> writer) {
            this.reader = reader;
            this.writer = writer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncQueueReader<L> getReader() {
            return reader;
        }

        public void setReader(AsyncQueueReader<L> reader) {
            this.reader = reader;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public AsyncQueueWriter<L> getWriter() {
            return writer;
        }
        
        public void setWriter(AsyncQueueWriter<L> writer) {
            this.writer = writer;
        }               
    }    
    
}
