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

import static org.glassfish.grizzly.ssl.SSLUtils.getSSLEngine;
import static org.glassfish.grizzly.ssl.SSLUtils.getSSLPacketSize;
import static org.glassfish.grizzly.ssl.SSLUtils.sslEngineUnwrap;

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
import org.glassfish.grizzly.memory.MemoryManager;

/**
 * <tt>Transformer</tt>, which decodes SSL encrypted data, contained in the input Buffer, to the output Buffer.
 *
 * @author Alexey Stashok
 */
public final class SSLDecoderTransformer extends AbstractTransformer<Buffer, Buffer> {
    public static final int NEED_HANDSHAKE_ERROR = 1;
    public static final int BUFFER_UNDERFLOW_ERROR = 2;
    public static final int BUFFER_OVERFLOW_ERROR = 3;

    private static final TransformationResult<Buffer, Buffer> HANDSHAKE_NOT_EXECUTED_RESULT = TransformationResult.createErrorResult(NEED_HANDSHAKE_ERROR,
            "Handshake was not executed");

    private static final Logger LOGGER = Grizzly.logger(SSLDecoderTransformer.class);

    private final MemoryManager memoryManager;

    public SSLDecoderTransformer() {
        this(MemoryManager.DEFAULT_MEMORY_MANAGER);
    }

    public SSLDecoderTransformer(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    @Override
    public String getName() {
        return SSLDecoderTransformer.class.getName();
    }

    @Override
    protected TransformationResult<Buffer, Buffer> transformImpl(AttributeStorage state, Buffer originalMessage) throws TransformationException {

        final SSLEngine sslEngine = getSSLEngine((Connection) state);
        if (sslEngine == null) {
            return HANDSHAKE_NOT_EXECUTED_RESULT;
        }

        final int expectedLength;
        try {
            expectedLength = getSSLPacketSize(originalMessage);
            if (expectedLength == -1 || originalMessage.remaining() < expectedLength) {
                return TransformationResult.createIncompletedResult(originalMessage);
            }
        } catch (SSLException e) {
            throw new TransformationException(e);
        }

        final Buffer targetBuffer = memoryManager.allocate(sslEngine.getSession().getApplicationBufferSize());

        TransformationResult<Buffer, Buffer> transformationResult = null;

        try {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SSLDecoder engine: {0} input: {1} output: {2}", new Object[] { sslEngine, originalMessage, targetBuffer });
            }

            final int pos = originalMessage.position();
            final SSLEngineResult sslEngineResult;
            if (!originalMessage.isComposite()) {
                sslEngineResult = sslEngineUnwrap(sslEngine, originalMessage.toByteBuffer(), targetBuffer.toByteBuffer());
            } else {
                final ByteBuffer originalByteBuffer = originalMessage.toByteBuffer(pos, pos + expectedLength);

                sslEngineResult = sslEngineUnwrap(sslEngine, originalByteBuffer, targetBuffer.toByteBuffer());
            }

            originalMessage.position(pos + sslEngineResult.bytesConsumed());
            targetBuffer.position(sslEngineResult.bytesProduced());

            final SSLEngineResult.Status status = sslEngineResult.getStatus();

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "SSLDecoderr done engine: {0} result: {1} input: {2} output: {3}",
                        new Object[] { sslEngine, sslEngineResult, originalMessage, targetBuffer });
            }

            if (status == SSLEngineResult.Status.OK) {
                targetBuffer.trim();

                return TransformationResult.createCompletedResult(targetBuffer, originalMessage);
            } else if (status == SSLEngineResult.Status.CLOSED) {
                targetBuffer.dispose();

                return TransformationResult.createCompletedResult(Buffers.EMPTY_BUFFER, originalMessage);
            } else {
                targetBuffer.dispose();

                if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
                    transformationResult = TransformationResult.createIncompletedResult(originalMessage);
                } else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW) {
                    transformationResult = TransformationResult.createErrorResult(BUFFER_OVERFLOW_ERROR, "Buffer overflow during unwrap operation");
                }
            }
        } catch (SSLException e) {
            targetBuffer.dispose();
            throw new TransformationException(e);
        }

        return transformationResult;
    }

    @Override
    public boolean hasInputRemaining(AttributeStorage storage, Buffer input) {
        return input != null && input.hasRemaining();
    }
}
