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
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.TransformationResult.Status;
import org.glassfish.grizzly.Transformer;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.impl.ReadyFutureImpl;
import org.glassfish.grizzly.memory.CompositeBuffer;
import org.glassfish.grizzly.memory.MemoryManager;
import java.io.IOException;

/**
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class TransformerOutput extends BufferedOutput {

    private final Attribute<CompositeBuffer> outputBufferAttr;
    protected final Transformer<Buffer, Buffer> transformer;
    protected final Output underlyingOutput;
    protected final MemoryManager memoryManager;
    protected final AttributeStorage attributeStorage;

    public TransformerOutput(Transformer<Buffer, Buffer> transformer,
            Output underlyingOutput, Connection connection) {
        this(transformer, underlyingOutput,
                connection.getMemoryManager(), connection);
    }

    public TransformerOutput(Transformer<Buffer, Buffer> transformer,
            Output underlyingOutput, MemoryManager memoryManager,
            AttributeStorage attributeStorage) {

        this.transformer = transformer;
        this.underlyingOutput = underlyingOutput;
        this.memoryManager = memoryManager;
        this.attributeStorage = attributeStorage;

        outputBufferAttr = Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(
                "TransformerOutput-" + transformer.getName());
    }

    @Override
    protected GrizzlyFuture<Integer> flush0(Buffer buffer,
            final CompletionHandler<Integer> completionHandler)
            throws IOException {
        
        if (buffer != null) {
            CompositeBuffer savedBuffer = outputBufferAttr.get(attributeStorage);
            if (savedBuffer != null) {
                savedBuffer.append(buffer);
                buffer = savedBuffer;
            }

            do {
                final TransformationResult<Buffer, Buffer> result =
                        transformer.transform(attributeStorage, buffer);
                final Status status = result.getStatus();

                if (status == Status.COMPLETE) {
                    final Buffer outputBuffer = result.getMessage();
                    underlyingOutput.write(outputBuffer);
                    transformer.release(attributeStorage);
                } else if (status == Status.INCOMPLETE) {
                    buffer.compact();
                    if (!buffer.isComposite()) {
                        buffer = CompositeBuffer.newBuffer(
                                memoryManager, buffer);
                    }
                    outputBufferAttr.set(attributeStorage, (CompositeBuffer) buffer);

                    return ReadyFutureImpl.create(
                            new IllegalStateException("Can not flush data: " +
                            "Insufficient input data for transformer"));
                } else if (status == Status.ERROR) {
                    transformer.release(attributeStorage);
                    throw new IOException("Transformation exception: "
                            + result.getErrorDescription());
                }
            } while (buffer.hasRemaining());

            return underlyingOutput.flush(completionHandler);
        }

        return ZERO_READY_FUTURE;
    }

    @Override
    protected Buffer newBuffer(int size) {
        return memoryManager.allocate(size);
    }

    @Override
    protected Buffer reallocateBuffer(Buffer oldBuffer, int size) {
        return memoryManager.reallocate(oldBuffer, size);
    }

    @Override
    protected void onClosed() throws IOException {
    }
}
