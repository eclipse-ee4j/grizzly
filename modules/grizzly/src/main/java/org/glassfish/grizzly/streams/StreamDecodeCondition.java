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

import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.TransformationResult.Status;
import org.glassfish.grizzly.Transformer;
import org.glassfish.grizzly.utils.ResultAware;
import org.glassfish.grizzly.utils.conditions.Condition;

/**
 *
 * @author Alexey Stashok
 */
public class StreamDecodeCondition<E> implements Condition {

    private final StreamReader streamReader;
    
    private final Transformer<Stream, E> decoder;
    private final ResultAware<E> resultAware;

    public StreamDecodeCondition(StreamReader streamReader,
            Transformer<Stream, E> decoder,
            ResultAware<E> resultAware) {
        this.streamReader = streamReader;
        this.decoder = decoder;
        this.resultAware = resultAware;
    }

    @Override
    public boolean check() {
        final TransformationResult<Stream, E> result =
                decoder.transform(streamReader.getConnection(), streamReader);

        final Status status = result.getStatus();
        if (status == Status.COMPLETE) {
            resultAware.setResult(result.getMessage());
            return true;
        } else if (status == Status.INCOMPLETE) {
            return false;
        }

        throw new TransformationException(result.getErrorCode() + ": " +
                result.getErrorDescription());
    }
}
