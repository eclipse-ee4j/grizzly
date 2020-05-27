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

package org.glassfish.grizzly.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.GrizzlyFuture;

/**
 *
 * @author Alexey Stashok
 */
public interface NIOChannelDistributor {
    void registerChannel(SelectableChannel channel) throws IOException;

    void registerChannel(SelectableChannel channel, int interestOps) throws IOException;

    void registerChannel(SelectableChannel channel, int interestOps, Object attachment) throws IOException;

    GrizzlyFuture<RegisterChannelResult> registerChannelAsync(SelectableChannel channel);

    GrizzlyFuture<RegisterChannelResult> registerChannelAsync(SelectableChannel channel, int interestOps);

    GrizzlyFuture<RegisterChannelResult> registerChannelAsync(SelectableChannel channel, int interestOps, Object attachment);

    void registerChannelAsync(SelectableChannel channel, int interestOps, Object attachment, CompletionHandler<RegisterChannelResult> completionHandler);

    void registerServiceChannelAsync(SelectableChannel channel, int interestOps, Object attachment, CompletionHandler<RegisterChannelResult> completionHandler);
}
