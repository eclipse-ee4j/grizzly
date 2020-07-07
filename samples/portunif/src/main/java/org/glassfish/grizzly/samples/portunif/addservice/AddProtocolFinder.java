/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.portunif.addservice;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.portunif.PUContext;
import org.glassfish.grizzly.portunif.ProtocolFinder;

/**
 * {@link ProtocolFinder}, responsible to determine if incoming byte buffer represents ADD-service request.
 *
 * @author Alexey Stashok
 */
public class AddProtocolFinder implements ProtocolFinder {

    /**
     * {@inheritDoc}
     */
    @Override
    public Result find(final PUContext puContext, final FilterChainContext ctx) {
        // Get the input Buffer
        final Buffer inputBuffer = ctx.getMessage();

        final int bytesToCompare = Math.min(AddServiceFilter.magic.length, inputBuffer.remaining());

        final int bufferStart = inputBuffer.position();

        // Compare incoming bytes with ADD-service protocol magic
        for (int i = 0; i < bytesToCompare; i++) {
            if (AddServiceFilter.magic[i] != inputBuffer.get(bufferStart + i)) {
                // If at least one byte doesn't match - it's not ADD-service protocol
                return Result.NOT_FOUND;
            }
        }

        // if we check entire magic - return FOUND, or NEED_MORE_DATA otherwise
        return bytesToCompare == AddServiceFilter.magic.length ? Result.FOUND : Result.NEED_MORE_DATA;
    }

}
