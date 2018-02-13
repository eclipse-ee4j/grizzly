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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

public class LZMAFilter extends BaseFilter {

    private final LZMAEncoder encoder = new LZMAEncoder();
    private final LZMADecoder decoder = new LZMADecoder();


    // ----------------------------------------------------- Methods from Filter


    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final Buffer input = ctx.getMessage();
        final TransformationResult<Buffer, Buffer> result =
                decoder.transform(connection, input);

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
                    decoder.finish(connection);
                    return ctx.getInvokeAction(remainder);
                }
                case INCOMPLETE: {
                    return ctx.getStopAction(remainder);
                }
                case ERROR: {
                    throw new IllegalStateException("LZMA decode error. Code: "
                            + result.getErrorCode() + " Description: "
                            + result.getErrorDescription());
                }
                default:
                    throw new IllegalStateException("Unexpected status: " +
                            result.getStatus());
            }
        } finally {
            result.recycle();
        }
    }

    @Override
    public NextAction handleWrite(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final Buffer input = ctx.getMessage();
        final TransformationResult<Buffer, Buffer> result =
                encoder.transform(connection, input);

        if (!input.hasRemaining()) {
            input.tryDispose();
        } else {
            input.shrink();
        }

        try {
            switch (result.getStatus()) {
                case COMPLETE:
                    encoder.finish(connection);
                case INCOMPLETE: {
                    final Buffer readyBuffer = result.getMessage();
                    if (readyBuffer != null) {
                        ctx.setMessage(readyBuffer);
                        return ctx.getInvokeAction();
                    } else {
                        return ctx.getStopAction();
                    }
                }

                case ERROR: {
                    throw new IllegalStateException("LZMA encode error. Code: "
                            + result.getErrorCode() + " Description: "
                            + result.getErrorDescription());
                }

                default:
                    throw new IllegalStateException("Unexpected status: " +
                            result.getStatus());
            }
        } finally {
            result.recycle();
        }
    }


    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        decoder.release(connection);
        encoder.release(connection);

        return super.handleClose(ctx);
    }

}
