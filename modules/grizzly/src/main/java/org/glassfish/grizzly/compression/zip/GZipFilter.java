/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.compression.zip;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.memory.Buffers;

/**
 * This class implements a {@link org.glassfish.grizzly.filterchain.Filter} which encodes/decodes data in the GZIP
 * format.
 *
 * @author Alexey Stashok
 */
public class GZipFilter extends BaseFilter {

    private final GZipDecoder decoder;
    private final GZipEncoder encoder;

    /**
     * Construct <tt>GZipFilter</tt> using default buffer sizes.
     */
    public GZipFilter() {
        this(512, 512);
    }

    /**
     * Construct <tt>GZipFilter</tt> using specific buffer sizes.
     * 
     * @param inBufferSize input buffer size
     * @param outBufferSize output buffer size
     */
    public GZipFilter(int inBufferSize, int outBufferSize) {
        this.decoder = new GZipDecoder(inBufferSize);
        this.encoder = new GZipEncoder(outBufferSize);
    }

    /**
     * Method perform the clean up of GZIP encoding/decoding state on a closed {@link Connection}.
     *
     * @param ctx Context of {@link FilterChainContext} processing.
     * @return the next action
     * @throws IOException
     */
    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        decoder.release(connection);
        encoder.release(connection);

        return super.handleClose(ctx);
    }

    /**
     * Method decodes GZIP encoded data stored in {@link FilterChainContext#getMessage()} and, as the result, produces a
     * {@link Buffer} with a plain data.
     * 
     * @param ctx Context of {@link FilterChainContext} processing.
     *
     * @return the next action
     * @throws IOException
     */
    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final Buffer input = ctx.getMessage();
        final TransformationResult<Buffer, Buffer> result = decoder.transform(connection, input);

        final Buffer remainder = result.getExternalRemainder();

        if (remainder == null) {
            input.tryDispose();
        } else {
            input.shrink();
        }

        try {
            switch (result.getStatus()) {
            case COMPLETE: {
                ctx.setMessage(result.getMessage());
                return ctx.getInvokeAction(remainder);
            }

            case INCOMPLETE: {
                return ctx.getStopAction(remainder);
            }

            case ERROR: {
                throw new IllegalStateException("GZip decode error. Code: " + result.getErrorCode() + " Description: " + result.getErrorDescription());
            }

            default:
                throw new IllegalStateException("Unexpected status: " + result.getStatus());
            }
        } finally {
            result.recycle();
        }
    }

    /**
     * Method compresses plain data stored in {@link FilterChainContext#getMessage()} and, as the result, produces a
     * {@link Buffer} with a GZIP compressed data.
     * 
     * @param ctx Context of {@link FilterChainContext} processing.
     *
     * @return the next action
     * @throws IOException
     */
    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final Buffer input = ctx.getMessage();
        final TransformationResult<Buffer, Buffer> result = encoder.transform(connection, input);

        input.dispose();

        try {
            switch (result.getStatus()) {
            case COMPLETE:
            case INCOMPLETE: {
                final Buffer readyBuffer = result.getMessage();
                final Buffer finishBuffer = encoder.finish(connection);

                final Buffer resultBuffer = Buffers.appendBuffers(connection.getMemoryManager(), readyBuffer, finishBuffer);

                if (resultBuffer != null) {
                    ctx.setMessage(resultBuffer);
                    return ctx.getInvokeAction();
                } else {
                    return ctx.getStopAction();
                }
            }

            case ERROR: {
                throw new IllegalStateException("GZip decode error. Code: " + result.getErrorCode() + " Description: " + result.getErrorDescription());
            }

            default:
                throw new IllegalStateException("Unexpected status: " + result.getStatus());
            }
        } finally {
            result.recycle();
        }
    }
}
