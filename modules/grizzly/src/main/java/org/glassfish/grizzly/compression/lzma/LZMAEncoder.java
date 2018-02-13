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

package org.glassfish.grizzly.compression.lzma;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.ThreadCache;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.compression.lzma.impl.Encoder;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;

import java.io.IOException;

public class LZMAEncoder extends AbstractTransformer<Buffer,Buffer> {

    private static final ThreadCache.CachedTypeIndex<LZMAOutputState> CACHE_IDX =
            ThreadCache.obtainIndex(LZMAOutputState.class, 2);

    private final LZMAProperties lzmaProperties;

    // ------------------------------------------------------------ Constructors


    public LZMAEncoder() {
        this(new LZMAProperties());
    }


    public LZMAEncoder(LZMAProperties lzmaProperties) {
        this.lzmaProperties = lzmaProperties;
    }


    // ---------------------------------------- Methods from AbstractTransformer


    @Override
    public String getName() {
        return "lzma-encoder";
    }


    @Override
    public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
         return input.hasRemaining();
    }


    @Override
    protected TransformationResult<Buffer, Buffer> transformImpl(AttributeStorage storage, Buffer input) throws TransformationException {
        final MemoryManager memoryManager = obtainMemoryManager(storage);
        final LZMAOutputState state = (LZMAOutputState) obtainStateObject(storage);

        if (!state.isInitialized()) {
            initializeOutput(state);
        }

        Buffer encodedBuffer = null;
        if (input != null && input.hasRemaining()) {
            try {
                state.setMemoryManager(memoryManager);
                encodedBuffer = encodeBuffer(input, state);
            } catch (IOException ioe) {
                throw new TransformationException(ioe);
            }
        }

        if (encodedBuffer == null) {
            return TransformationResult.createIncompletedResult(null);
        }

        return TransformationResult.createCompletedResult(encodedBuffer, null);

    }


    @Override
    protected LastResultAwareState<Buffer,Buffer> createStateObject() {
        return create();
    }


    // ---------------------------------------------------------- Public Methods


    public static LZMAOutputState create() {
        final LZMAOutputState state =
                ThreadCache.takeFromCache(CACHE_IDX);
        if (state != null) {
            return state;
        }

        return new LZMAOutputState();
    }

    public void finish(AttributeStorage storage) {
        final LZMAOutputState state = (LZMAOutputState) obtainStateObject(storage);
        state.recycle();
    }


    // --------------------------------------------------------- Private Methods


    private void initializeOutput(final LZMAOutputState state) {
        final Encoder encoder = state.getEncoder();
        encoder.setAlgorithm(lzmaProperties.getAlgorithm());
        encoder.setDictionarySize(lzmaProperties.getDictionarySize());
        encoder.setNumFastBytes(lzmaProperties.getNumFastBytes());
        encoder.setMatchFinder(lzmaProperties.getMatchFinder());
        encoder.setLcLpPb(lzmaProperties.getLc(), lzmaProperties.getLp(), lzmaProperties.getPb());
        encoder.setEndMarkerMode(true);
        state.setInitialized(true);
    }


    private Buffer encodeBuffer(Buffer input,
                                LZMAOutputState state) throws IOException {

        Buffer resultBuffer = null;

        state.setSrc(input);
        final Buffer encoded = encode(state);
        if (encoded != null) {
            resultBuffer = Buffers.appendBuffers(state.getMemoryManager(),
                    resultBuffer,
                    encoded);
        }

        input.position(input.limit());

        return resultBuffer;
    }


    private Buffer encode(LZMAOutputState outputState)
    throws IOException {


        final Encoder encoder = outputState.getEncoder();
        Buffer dst = outputState.getMemoryManager().allocate(512);
        outputState.setDst(dst);

        if (!outputState.isHeaderWritten()) {
            // writes a 5-byte header that the decoder will use in order
            // to achieve parity with the encoder's properties.
            encoder.writeCoderProperties(outputState.getDst());
            outputState.setHeaderWritten(true);
        }

        encoder.code(outputState, -1, -1);
        dst = outputState.getDst();
        int len = dst.position();
        if (len <= 0) {
            dst.dispose();
            return null;
        }

        dst.trim();

        return dst;
    }


    // ---------------------------------------------------------- Nested Classes


    public static class LZMAOutputState extends LastResultAwareState<Buffer,Buffer> implements Cacheable {

        private boolean initialized;

        /**
         * Compressor for this stream.
         */
        private final Encoder encoder = new Encoder();
        private Buffer dst;
        private Buffer src;
        private boolean headerWritten = false;
        private MemoryManager mm;

        public boolean isInitialized() {
            return initialized;
        }

        public void setInitialized(boolean initialized) {
            this.initialized = initialized;
        }

        public Buffer getSrc() {
            return src;
        }

        public void setSrc(Buffer src) {
            this.src = src;
        }

        public Buffer getDst() {
            return dst;
        }

        public void setDst(Buffer dst) {
            this.dst = dst;
        }

        public Encoder getEncoder() {
            return encoder;
        }

        public boolean isHeaderWritten() {
            return headerWritten;
        }

        public void setHeaderWritten(boolean headerWritten) {
            this.headerWritten = headerWritten;
        }

        public MemoryManager getMemoryManager() {
            return mm;
        }

        public void setMemoryManager(MemoryManager mm) {
            this.mm = mm;
        }

        @Override
        public void recycle() {
            lastResult = null;
            initialized = false;
            headerWritten = false;
            mm = null;
            dst = null;
            src = null;
            ThreadCache.putToCache(CACHE_IDX, this);
        }

    }

    public static class LZMAProperties {

        private int algorithm = 2;
        private int dictionarySize = 1 << 16;
        private int numFastBytes = 128;
        private int matchFinder = 1;
        private int lc = 3;
        private int lp = 0;
        private int pb = 2;

        public LZMAProperties() {
            loadProperties(this);
        }

        public LZMAProperties(int algorithm,
                              int dictionarySize,
                              int numFastBytes,
                              int matchFinder,
                              int lc,
                              int lp,
                              int pb) {
            this.algorithm = algorithm;
            this.dictionarySize = dictionarySize;
            this.numFastBytes = numFastBytes;
            this.matchFinder = matchFinder;
            this.lc = lc;
            this.lp = lp;
            this.pb = pb;
        }

        public int getLc() {
            return lc;
        }

        public void setLc(int Lc) {
            this.lc = Lc;
        }

        public int getLp() {
            return lp;
        }

        public void setLp(int Lp) {
            this.lp = Lp;
        }

        public int getPb() {
            return pb;
        }

        public void setPb(int Pb) {
            this.pb = Pb;
        }

        public int getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(int algorithm) {
            this.algorithm = algorithm;
        }

        public int getDictionarySize() {
            return dictionarySize;
        }

        public void setDictionarySize(int dictionarySize) {
            this.dictionarySize = dictionarySize;
        }

        public int getMatchFinder() {
            return matchFinder;
        }

        public void setMatchFinder(int matchFinder) {
            this.matchFinder = matchFinder;
        }

        public int getNumFastBytes() {
            return numFastBytes;
        }

        public void setNumFastBytes(int numFastBytes) {
            this.numFastBytes = numFastBytes;
        }

        public static void loadProperties(LZMAProperties properties) {
            properties.algorithm = Integer.getInteger("lzma-filter.algorithm", 2);
            properties.dictionarySize = 1 << Integer.getInteger("lzma-filter.dictionary-size", 16);
            properties.numFastBytes = Integer.getInteger("lzma-filter.num-fast-bytes", 128);
            properties.matchFinder = Integer.getInteger("lzma-filter.match-finder", 1);

            properties.lc = Integer.getInteger("lzma-filter.lc", 3);
            properties.lp = Integer.getInteger("lzma-filter.lp", 0);
            properties.pb = Integer.getInteger("lzma-filter.pb", 2);
        }

    } // END LZMAProperties

}
