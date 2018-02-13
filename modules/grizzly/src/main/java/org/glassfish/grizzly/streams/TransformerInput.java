/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.streams;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.TransformationResult.Status;
import org.glassfish.grizzly.Transformer;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.grizzly.utils.conditions.Condition;
import java.io.IOException;

/**
 *
 * @author Alexey Stashok
 */
public final class TransformerInput extends BufferedInput {

    private final Attribute<CompositeBuffer> inputBufferAttr;
    protected final Transformer<Buffer, Buffer> transformer;
    protected final Input underlyingInput;
    protected final MemoryManager memoryManager;
    protected final AttributeStorage attributeStorage;

    public TransformerInput(Transformer<Buffer, Buffer> transformer,
            Input underlyingInput, Connection connection) {
        this(transformer, underlyingInput,
                connection.getMemoryManager(), connection);
    }

    public TransformerInput(Transformer<Buffer, Buffer> transformer,
            Input underlyingInput, MemoryManager memoryManager,
            AttributeStorage attributeStorage) {

        this.transformer = transformer;
        this.underlyingInput = underlyingInput;
        this.memoryManager = memoryManager;
        this.attributeStorage = attributeStorage;

        inputBufferAttr = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
                "TransformerInput-" + transformer.getName());
    }

    @Override
    protected void onOpenInputSource() throws IOException {
        underlyingInput.notifyCondition(new TransformerCondition(),
                new TransformerCompletionHandler());
    }

    @Override
    protected void onCloseInputSource() throws IOException {
    }

    public final class TransformerCompletionHandler
            extends EmptyCompletionHandler<Integer> {
        
        @Override
        public void failed(Throwable throwable) {
            notifyFailure(completionHandler, throwable);
            future.failure(throwable);
        }
    }

    public final class TransformerCondition implements Condition {

        @Override
        public boolean check() {
            try {
                CompositeBuffer savedBuffer = inputBufferAttr.get(attributeStorage);
                Buffer bufferToTransform = savedBuffer;
                Buffer chunkBuffer;

                final boolean hasSavedBuffer = (savedBuffer != null);

                if (underlyingInput.isBuffered()) {
                    chunkBuffer = underlyingInput.takeBuffer();
                } else {
                    int size = underlyingInput.size();
                    chunkBuffer = memoryManager.allocate(size);
                    while (size-- >= 0) {
                        chunkBuffer.put(underlyingInput.read());
                    }
                    chunkBuffer.flip();
                }

                if (hasSavedBuffer) {
                    savedBuffer.append(chunkBuffer);
                } else {
                    bufferToTransform = chunkBuffer;
                }

                while (bufferToTransform.hasRemaining()) {
                    final TransformationResult<Buffer, Buffer> result =
                            transformer.transform(attributeStorage,
                            bufferToTransform);
                    final Status status = result.getStatus();

                    if (status == Status.COMPLETE) {
                        final Buffer outputBuffer = result.getMessage();
                        lock.writeLock().lock();
                        try {
                            append(outputBuffer);

                            if (!isCompletionHandlerRegistered) {
                                // if !isCompletionHandlerRegistered - it means StreamReader has enough data to continue processing
                                return true;
                            }
                        } finally {
                            lock.writeLock().unlock();
                        }
                    } else if (status == Status.INCOMPLETE) {
                        if (!hasSavedBuffer) {
                            if (bufferToTransform.isComposite()) {
                                inputBufferAttr.set(attributeStorage,
                                        (CompositeBuffer) bufferToTransform);
                            } else {
                                savedBuffer = CompositeBuffer.newBuffer(memoryManager);
                                savedBuffer.append(bufferToTransform);
                                inputBufferAttr.set(attributeStorage, savedBuffer);
                            }
                        }

                        return false;
                    } else if (status == Status.ERROR) {
                        throw new TransformationException(result.getErrorDescription());
                    }
                }

                return false;
            } catch (IOException e) {
                throw new TransformationException(e);
            }
        }
    }
}
