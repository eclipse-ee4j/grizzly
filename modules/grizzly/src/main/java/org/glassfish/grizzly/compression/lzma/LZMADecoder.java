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
import org.glassfish.grizzly.compression.lzma.impl.Decoder;
import org.glassfish.grizzly.memory.MemoryManager;

import java.io.IOException;
import org.glassfish.grizzly.compression.lzma.impl.Base;
import org.glassfish.grizzly.compression.lzma.impl.Decoder.LiteralDecoder;

public class LZMADecoder extends AbstractTransformer<Buffer,Buffer> {

    private static final ThreadCache.CachedTypeIndex<LZMAInputState> CACHE_IDX =
            ThreadCache.obtainIndex(LZMAInputState.class, 2);


    // ---------------------------------------- Methods from AbstractTransformer


    @Override
    public String getName() {
        return "lzma-decoder";
    }

    @Override
    public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
        return input.hasRemaining();
    }

    @Override
    protected TransformationResult<Buffer, Buffer> transformImpl(AttributeStorage storage, Buffer input) throws TransformationException {
        final MemoryManager memoryManager = obtainMemoryManager(storage);

        final LZMAInputState state = (LZMAInputState) obtainStateObject(storage);
        state.setMemoryManager(memoryManager);

        Buffer decodedBuffer = null;
        Decoder.State decState = null;
        if (input.hasRemaining()) {
            decState = decodeBuffer(memoryManager, input, state);
            decodedBuffer = state.getDst();
        }

        final boolean hasRemainder = input.hasRemaining();

        if (decState == Decoder.State.NEED_MORE_DATA
                || decodedBuffer == null) {
            return TransformationResult.createIncompletedResult(hasRemainder ? input : null);
        }

        return TransformationResult.createCompletedResult(decodedBuffer.flip(),
                hasRemainder ? input : null);
    }

    @Override
    protected LastResultAwareState<Buffer, Buffer> createStateObject() {
        return create();
    }

    // ---------------------------------------------------------- Public Methods


    public static LZMAInputState create() {
        final LZMAInputState state =
                ThreadCache.takeFromCache(CACHE_IDX);
        if (state != null) {
            return state;
        }

        return new LZMAInputState();
    }


    public void finish(AttributeStorage storage) {
        final LZMAInputState state = (LZMAInputState) obtainStateObject(storage);
        state.recycle();
    }


    // --------------------------------------------------------- Private Methods


    private Decoder.State decodeBuffer(final MemoryManager memoryManager,
                                       final Buffer buffer,
                                       final LZMAInputState state) {

        state.setSrc(buffer);

        Decoder.State decState;
        try {
            decState = state.getDecoder().code(state, -1);
        } catch (IOException e) {
            disposeDstBuffer(state);
            throw new IllegalStateException(e);
        }
        if (decState == Decoder.State.ERR) {
            disposeDstBuffer(state);
            throw new IllegalStateException("Invalid decoder state.");
        }

        return decState;

    }

    private static void disposeDstBuffer(LZMAInputState state) {
        final Buffer dstBuffer = state.getDst();
        if (dstBuffer != null) {
            dstBuffer.dispose();
            state.setDst(null);
        }
    }


    // ---------------------------------------------------------- Nested Classes


    public static class LZMAInputState extends LastResultAwareState<Buffer,Buffer> implements Cacheable {

        private final Decoder decoder = new Decoder();
        private boolean initialized;
        private final byte[] decoderConfigBits = new byte[5];
        private Buffer src;
        private Buffer dst;
        private MemoryManager mm;

        public int state;
        public int rep0;
        public int rep1;
        public int rep2;
        public int rep3;
        public long nowPos64;
        public byte prevByte;
        public boolean decInitialized;
        
        public int posState;
        public int lastMethodResult;

        public int inner1State;
        public int inner2State;

        public LiteralDecoder.Decoder2 decoder2;

        // BitTreeDecoder static reverseDecode state
        public int staticReverseDecodeMethodState;
        public int staticM;
        public int staticBitIndex;
        public int staticSymbol;

        // Decoder.processState3 method state
        public int state3Len;
        
        // Decoder.processState31 method state
        public int state31;

        // Decoder.processState311 method state
        public int state311;
        public int state311Distance;

        // Decoder.processState32 method state
        public int state32;
        public int state32PosSlot;

        // Decoder.processState321 method state
        public int state321;
        public int state321NumDirectBits;

        // ------------------------------------------------------ Public Methods


        public boolean initialize(final Buffer buffer) {
            buffer.get(decoderConfigBits);
            initialized = decoder.setDecoderProperties(decoderConfigBits);
            state = Base.stateInit();
            return initialized;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public Decoder getDecoder() {
            return decoder;
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

        public MemoryManager getMemoryManager() {
            return mm;
        }

        public void setMemoryManager(MemoryManager mm) {
            this.mm = mm;
        }

        // ---------------------------------------------- Methods from Cacheable


        @Override
        public void recycle() {
            state = 0;
            rep0 = 0;
            rep1 = 0;
            rep2 = 0;
            rep3 = 0;
            nowPos64 = 0;
            prevByte = 0;
            src = null;
            dst = null;
            lastResult = null;
            initialized = false;
            decInitialized = false;
            mm = null;

            posState = 0;
            lastMethodResult = 0;

            inner1State = 0;
            inner2State = 0;

            decoder2 = null;

            staticReverseDecodeMethodState = 0;

            state31 = 0;
            state311 = 0;
            state32 = 0;
            state321 = 0;

            ThreadCache.putToCache(CACHE_IDX, this);
        }

    } // END LZMAInputState

}
