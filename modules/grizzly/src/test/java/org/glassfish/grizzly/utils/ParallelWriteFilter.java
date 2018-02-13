/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.utils;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ParallelWriteFilter extends BaseFilter {
    
        private static final Logger LOGGER = Grizzly.logger(ParallelWriteFilter.class);
        private final int packetsNumber;
        private final int size;

        private final ExecutorService executorService;

        public ParallelWriteFilter(ExecutorService executorService,
                                   int packetsNumber, int size) {
            this.executorService = executorService;
            this.packetsNumber = packetsNumber;
            this.size = size;
        }

        @Override
        public NextAction handleRead(final FilterChainContext ctx) throws IOException {
            final Connection connection = ctx.getConnection();
            for (int i = 0; i < packetsNumber; i++) {
                final int packetNumber = i;

                executorService.submit(new Runnable() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public void run() {
                        final char[] chars = new char[size];
                        Arrays.fill(chars, (char) ('0' + (packetNumber % 10)));
                        final String content = new String(chars);
                        final FutureImpl<Boolean> completionHandlerFuture =
                                SafeFutureImpl.create();
                        try {

                            connection.write(content, new CompletionHandler<WriteResult>() {
                                @Override
                                public void cancelled() {
                                    completionHandlerFuture.failure(new IOException("cancelled"));
                                }

                                @Override
                                public void failed(Throwable throwable) {
                                    completionHandlerFuture.failure(throwable);
                                }

                                @Override
                                public void completed(WriteResult result) {
                                    completionHandlerFuture.result(true);
                                }

                                @Override
                                public void updated(WriteResult result) {
                                }
                            });

                            completionHandlerFuture.get(10, TimeUnit.SECONDS);

                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "sending packet #" + packetNumber, e);
                        }
                    }
                });
            }

            return ctx.getInvokeAction();
        }
    }
