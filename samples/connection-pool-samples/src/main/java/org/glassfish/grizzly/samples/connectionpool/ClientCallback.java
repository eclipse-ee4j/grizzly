/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.connectionpool;

import org.glassfish.grizzly.Connection;

/**
 * The general callback interface to be used by a {@link ClientFilter} to delegate client {@link Connection}'s event
 * processing.
 */
public interface ClientCallback {
    /**
     * The callback operation will be executed once new client-side {@link Connection} is established.
     *
     * @param connection the new {@link Connection}
     */
    void onConnectionEstablished(Connection connection);

    /**
     * The callback operation will be executed once a client {@link Connection} receives response from a server.
     *
     * @param connection the client-side {@link Connection}
     * @param responseMessage the response
     */
    void onResponseReceived(Connection connection, String responseMessage);
}
