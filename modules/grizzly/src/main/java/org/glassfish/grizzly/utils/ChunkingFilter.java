/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.utils;

import java.util.logging.Logger;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.memory.Buffers;

/**
 * The Filter is responsible to break the incoming/outgoing data into chunks and pass them down/up by the
 * {@link FilterChain}. This Filter could be useful for testing reasons to check if all Filters in the
 * {@link FilterChain} work properly with chunked data.
 *
 * @author Alexey Stashok
 */
public class ChunkingFilter extends AbstractCodecFilter<Buffer, Buffer> {
    private static final Logger LOGGER = Grizzly.logger(ChunkingFilter.class);

    private final int chunkSize;

    /**
     * Construct a <tt>ChunkFilter</tt>, which will break incoming/outgoing data into chunks of the specified size.
     *
     * @param chunkSize the chunk size.
     */
    public ChunkingFilter(int chunkSize) {
        super(new ChunkingDecoder(chunkSize), new ChunkingEncoder(chunkSize));
        this.chunkSize = chunkSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public static final class ChunkingDecoder extends ChunkingTransformer {

        public ChunkingDecoder(int chunk) {
            super(chunk);
        }

    }

    public static final class ChunkingEncoder extends ChunkingTransformer {

        public ChunkingEncoder(int chunk) {
            super(chunk);
        }

    }

    public static abstract class ChunkingTransformer extends AbstractTransformer<Buffer, Buffer> {
        private final int chunk;

        public ChunkingTransformer(int chunk) {
            this.chunk = chunk;
        }

        @Override
        public String getName() {
            return "ChunkingTransformer";
        }

        @Override
        protected TransformationResult<Buffer, Buffer> transformImpl(AttributeStorage storage, Buffer input) throws TransformationException {

            if (!input.hasRemaining()) {
                return TransformationResult.createIncompletedResult(input);
            }

            final int chunkSize = Math.min(chunk, input.remaining());

            final int oldInputPos = input.position();
            final int oldInputLimit = input.limit();

            Buffers.setPositionLimit(input, oldInputPos, oldInputPos + chunkSize);

            final Buffer output = obtainMemoryManager(storage).allocate(chunkSize);
            output.put(input).flip();

            Buffers.setPositionLimit(input, oldInputPos + chunkSize, oldInputLimit);

            return TransformationResult.createCompletedResult(output, input);
        }

        @Override
        public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
            return input != null && input.hasRemaining();
        }
    }
}
