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

package org.glassfish.grizzly.comet;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.glassfish.grizzly.http.server.Response;

public class CountDownCometHandler extends DefaultTestCometHandler {
    public final CountDownLatch onEvent;
    public final CountDownLatch onInitialize;
    public final CountDownLatch onInterrupt;
    public final CountDownLatch onTerminate;

    public CountDownCometHandler(CometContext<String> cometContext, Response response) {
        super(cometContext, response, false);
        onEvent = new CountDownLatch(1);
        onInitialize = new CountDownLatch(1);
        onInterrupt = new CountDownLatch(1);
        onTerminate = new CountDownLatch(1);
    }

    @Override
    public void onInterrupt(CometEvent event) throws IOException {
        super.onInterrupt(event);
        onInterrupt.countDown();
    }

    @Override
    public void onTerminate(CometEvent event) throws IOException {
        super.onTerminate(event);
        onTerminate.countDown();
    }

    @Override
    public void onEvent(CometEvent event) throws IOException {
        super.onEvent(event);
        onEvent.countDown();
    }

    @Override
    public void onInitialize(CometEvent event) throws IOException {
        super.onInitialize(event);
        onInitialize.countDown();
    }
}
