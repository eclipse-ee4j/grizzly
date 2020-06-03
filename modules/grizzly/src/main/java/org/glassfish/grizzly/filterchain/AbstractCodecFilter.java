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

package org.glassfish.grizzly.filterchain;

import java.io.IOException;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.Transformer;

/**
 *
 * @author Alexey Stashok
 */
public abstract class AbstractCodecFilter<K, L> extends BaseFilter implements CodecFilter<K, L> {
    private final Transformer<K, L> decoder;
    private final Transformer<L, K> encoder;

    public AbstractCodecFilter(final Transformer<K, L> decoder, final Transformer<L, K> encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final K message = ctx.getMessage();

        final TransformationResult<K, L> result = decoder.transform(connection, message);

        switch (result.getStatus()) {
        case COMPLETE:
            final K remainder = result.getExternalRemainder();
            final boolean hasRemaining = decoder.hasInputRemaining(connection, remainder);
            decoder.release(connection);
            ctx.setMessage(result.getMessage());
            return hasRemaining ? ctx.getInvokeAction(remainder) : ctx.getInvokeAction();
        case INCOMPLETE:
            return ctx.getStopAction(message);
        case ERROR:
            throw new TransformationException(getClass().getName() + " transformation error: (" + result.getErrorCode() + ") " + result.getErrorDescription());
        }

        return ctx.getInvokeAction();
    }

    @Override
    @SuppressWarnings("unchecked")
    public NextAction handleWrite(final FilterChainContext ctx) throws IOException {
        final Connection connection = ctx.getConnection();
        final L message = ctx.getMessage();

        final TransformationResult<L, K> result = encoder.transform(connection, message);

        switch (result.getStatus()) {
        case COMPLETE:
            ctx.setMessage(result.getMessage());
            final L remainder = result.getExternalRemainder();
            final boolean hasRemaining = encoder.hasInputRemaining(connection, remainder);
            encoder.release(connection);
            return hasRemaining ? ctx.getInvokeAction(remainder) : ctx.getInvokeAction();
        case INCOMPLETE:
            return ctx.getStopAction(message);
        case ERROR:
            throw new TransformationException(getClass().getName() + " transformation error: (" + result.getErrorCode() + ") " + result.getErrorDescription());
        }

        return ctx.getInvokeAction();
    }

    @Override
    public Transformer<K, L> getDecoder() {
        return decoder;
    }

    @Override
    public Transformer<L, K> getEncoder() {
        return encoder;
    }
}
