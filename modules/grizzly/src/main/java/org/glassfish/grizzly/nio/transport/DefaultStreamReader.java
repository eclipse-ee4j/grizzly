/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio.transport;

import java.io.IOException;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Interceptor;
import org.glassfish.grizzly.ReadResult;
import org.glassfish.grizzly.Reader;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.streams.AbstractStreamReader;
import org.glassfish.grizzly.streams.BufferedInput;

/**
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public final class DefaultStreamReader extends AbstractStreamReader {

    public DefaultStreamReader(Connection connection) {
        super(connection, new Input());
        ((Input) input).parentStreamReader = this;
    }

    public Input getSource() {
        return (Input) input;
    }

    public static final class Input extends BufferedInput {

        private DefaultStreamReader parentStreamReader;
        private InputInterceptor interceptor;

        @Override
        protected void onOpenInputSource() throws IOException {
            final Connection connection = parentStreamReader.getConnection();
            final Transport transport = connection.getTransport();
            final Reader reader = transport.getReader(connection);

            interceptor = new InputInterceptor();
            reader.read(connection, null, null, interceptor);
        }

        @Override
        protected void onCloseInputSource() throws IOException {
            interceptor.isDone = true;
            interceptor = null;
        }

        @Override
        protected void notifyCompleted(final CompletionHandler<Integer> completionHandler) {
            if (completionHandler != null) {
                completionHandler.completed(compositeBuffer.remaining());
            }
        }

        @Override
        protected void notifyFailure(final CompletionHandler<Integer> completionHandler, final Throwable failure) {
            if (completionHandler != null) {
                completionHandler.failed(failure);
            }
        }

        private class InputInterceptor implements Interceptor<ReadResult<Buffer, ?>> {

            boolean isDone = false;

            @Override
            public int intercept(int event, Object context, ReadResult<Buffer, ?> result) {
                if (event == Reader.READ_EVENT) {
                    final Buffer buffer = result.getMessage();
                    result.setMessage(null);

                    if (buffer == null) {
                        return Interceptor.INCOMPLETED;
                    }

                    buffer.trim();
                    append(buffer);
                    if (isDone) {
                        return Interceptor.COMPLETED;
                    }

                    return Interceptor.INCOMPLETED | Interceptor.RESET;
                }

                return Interceptor.DEFAULT;
            }
        }
    }
}
