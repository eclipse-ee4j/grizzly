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

package org.glassfish.grizzly.ssl;

import static org.glassfish.grizzly.ssl.SSLUtils.sslEngineWrap;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.ByteBufferArray;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * <tt>Transformer</tt>, which encrypts plain data, contained in the input Buffer, into SSL/TLS data and puts the result
 * to the output Buffer.
 *
 * @author Alexey Stashok
 */
public final class SSLEncoderTransformer extends AbstractTransformer<Buffer, Buffer> {

    public static final int NEED_HANDSHAKE_ERROR = 1;
    public static final int BUFFER_UNDERFLOW_ERROR = 2;
    public static final int BUFFER_OVERFLOW_ERROR = 3;

    private static final Logger LOGGER = Grizzly.logger(SSLEncoderTransformer.class);

    private static final TransformationResult<Buffer, Buffer> HANDSHAKE_NOT_EXECUTED_RESULT = TransformationResult.createErrorResult(NEED_HANDSHAKE_ERROR,
            "Handshake was not executed");

    private final MemoryManager memoryManager;

    public SSLEncoderTransformer() {
        this(MemoryManager.DEFAULT_MEMORY_MANAGER);
    }

    public SSLEncoderTransformer(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String getName() {
        return SSLEncoderTransformer.class.getName();
    }

    @Override
    protected TransformationResult<Buffer, Buffer> transformImpl(final AttributeStorage state, final Buffer originalMessage) throws TransformationException {

        final SSLEngine sslEngine = SSLUtils.getSSLEngine((Connection) state);
        if (sslEngine == null) {
            return HANDSHAKE_NOT_EXECUTED_RESULT;
        }

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) { // synchronize parallel writers here
            return wrapAll(sslEngine, originalMessage);
        }
    }

    private TransformationResult<Buffer, Buffer> wrapAll(final SSLEngine sslEngine, final Buffer originalMessage) throws TransformationException {

        TransformationResult<Buffer, Buffer> transformationResult = null;

        Buffer targetBuffer = null;
        Buffer currentTargetBuffer = null;

        final ByteBufferArray originalByteBufferArray = originalMessage.toByteBufferArray();
        boolean restore = false;
        for (int i = 0; i < originalByteBufferArray.size(); i++) {
            final int pos = originalMessage.position();
            final ByteBuffer originalByteBuffer = originalByteBufferArray.getArray()[i];

            currentTargetBuffer = allowDispose(memoryManager.allocate(sslEngine.getSession().getPacketBufferSize()));

            final ByteBuffer currentTargetByteBuffer = currentTargetBuffer.toByteBuffer();

            try {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "SSLEncoder engine: {0} input: {1} output: {2}",
                            new Object[] { sslEngine, originalByteBuffer, currentTargetByteBuffer });
                }

                final SSLEngineResult sslEngineResult = sslEngineWrap(sslEngine, originalByteBuffer, currentTargetByteBuffer);

                // If the position of the original message hasn't changed,
                // update the position now.
                if (pos == originalMessage.position()) {
                    restore = true;
                    originalMessage.position(pos + sslEngineResult.bytesConsumed());
                }

                final SSLEngineResult.Status status = sslEngineResult.getStatus();

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "SSLEncoder done engine: {0} result: {1} input: {2} output: {3}",
                            new Object[] { sslEngine, sslEngineResult, originalByteBuffer, currentTargetByteBuffer });
                }

                if (status == SSLEngineResult.Status.OK) {
                    currentTargetBuffer.position(sslEngineResult.bytesProduced());
                    currentTargetBuffer.trim();
                    targetBuffer = Buffers.appendBuffers(memoryManager, targetBuffer, currentTargetBuffer);

                } else if (status == SSLEngineResult.Status.CLOSED) {
                    transformationResult = TransformationResult.createCompletedResult(Buffers.EMPTY_BUFFER, originalMessage);
                    break;
                } else {
                    if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                        transformationResult = TransformationResult.createErrorResult(BUFFER_UNDERFLOW_ERROR, "Buffer underflow during wrap operation");
                    } else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                        transformationResult = TransformationResult.createErrorResult(BUFFER_OVERFLOW_ERROR, "Buffer overflow during wrap operation");
                    }
                    break;
                }
            } catch (SSLException e) {
                disposeBuffers(currentTargetBuffer, targetBuffer);

                originalByteBufferArray.restore();

                throw new TransformationException(e);
            }

            if (originalByteBuffer.hasRemaining()) { // Keep working with the current source ByteBuffer
                i--;
            }
        }
        assert !originalMessage.hasRemaining();

        if (restore) {
            originalByteBufferArray.restore();
        }
        originalByteBufferArray.recycle();

        if (transformationResult != null) { // transformation error case
            disposeBuffers(currentTargetBuffer, targetBuffer);

            return transformationResult;
        }

        return TransformationResult.createCompletedResult(allowDispose(targetBuffer), originalMessage);
    }

    private static void disposeBuffers(final Buffer currentBuffer, final Buffer bigBuffer) {
        if (currentBuffer != null) {
            currentBuffer.dispose();
        }

        if (bigBuffer != null) {
            bigBuffer.allowBufferDispose(true);
            if (bigBuffer.isComposite()) {
                ((CompositeBuffer) bigBuffer).allowInternalBuffersDispose(true);
            }

            bigBuffer.dispose();
        }
    }

    private static Buffer allowDispose(final Buffer buffer) {
        buffer.allowBufferDispose(true);
        if (buffer.isComposite()) {
            ((CompositeBuffer) buffer).allowInternalBuffersDispose(true);
        }

        return buffer;
    }

    @Override
    public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
        return input != null && input.hasRemaining();
    }
}
