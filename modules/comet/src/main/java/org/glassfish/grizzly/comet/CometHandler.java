/*
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.comet;

import java.io.IOException;

import org.glassfish.grizzly.http.server.Response;

/**
 * This interface represents a suspended connection (or response). Passing an instance of this class to
 * {@link CometContext#addCometHandler(CometHandler)} automatically tells Grizzly Comet to suspend the underlying
 * connection and to avoid committing the response. Since the response is not committed, the connection is considered as
 * suspended and can be resumed later when an event happens by invoking
 * {@link CometContext#resumeCometHandler(CometHandler)}, from {@link CometHandler#onEvent}.
 * {@link CometContext#resumeCometHandler(CometHandler)}, resume the connection by committing the response. As an
 * example, a browser icons will spins when a connection is suspended, as the complete response hasn't been sent back.
 * <p/>
 * Components that implement this interface will be notified {@link org.glassfish.grizzly.comet.CometContext#notify()}
 * is invoked or when the {@link CometContext#getExpirationDelay()} expires.
 */
public interface CometHandler<E> {
    /**
     * @return the response associated with the handler.
     */
    Response getResponse();

    void setResponse(Response response);

    CometContext<E> getCometContext();

    void setCometContext(CometContext<E> context);

    /**
     * Receive {@link CometEvent} notification. This method will be invoked every time a {@link CometContext#notify} is
     * invoked. The {@link CometEvent} will contains the message that can be pushed back to the remote client, cached or
     * ignored. This method can also be used to resume a connection once a notified by invoking
     * {@link CometContext#resumeCometHandler}.<br>
     * its not optimal to flush outputstream in this method for long polling, flush is performed in each CometContext.resume
     * call.<br>
     * flushing multiple times can fragment the data into several tcp packets, that leads to extra IO and overhead in
     * general due to client ack for each packet etc.
     */
    void onEvent(CometEvent event) throws IOException;

    /**
     * Receive {@link CometEvent} notification when Grizzly is about to suspend the connection. This method is always
     * invoked during the processing of {@link CometContext#addCometHandler} operations.
     */
    void onInitialize(CometEvent event) throws IOException;

    /**
     * Receive {@link CometEvent} notification when the response is resumed by a {@link CometHandler} or by the
     * {@link CometContext}
     */
    void onTerminate(CometEvent event) throws IOException;

    /**
     * Receive {@link CometEvent} notification when the underlying tcp communication is resumed by Grizzly. This happens
     * when the {@link CometContext#setExpirationDelay} expires or when the remote client close the connection.
     */
    void onInterrupt(CometEvent event) throws IOException;
}
