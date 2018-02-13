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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Grizzly;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.http.server.Response;

public class DefaultTestCometHandler extends DefaultCometHandler<String> implements Comparable<CometHandler> {
    private static final Logger LOGGER = Grizzly.logger(DefaultTestCometHandler.class);

    volatile AtomicBoolean onInitializeCalled = new AtomicBoolean(false);
    volatile AtomicBoolean onInterruptCalled = new AtomicBoolean(false);
    volatile AtomicBoolean onEventCalled = new AtomicBoolean(false);
    volatile AtomicBoolean onTerminateCalled = new AtomicBoolean(false);
    
    private final boolean resumeAfterEvent;
    
    public DefaultTestCometHandler(CometContext<String> cometContext,
            Response response, boolean resume) {
        super(cometContext, response);
        this.resumeAfterEvent = resume;
    }

    public void onEvent(CometEvent event) throws IOException {
        LOGGER.log(Level.FINE, "     -> onEvent Handler:{0}", hashCode());
        onEventCalled.set(true);
        if (resumeAfterEvent) {
            getCometContext().resumeCometHandler(this);
        }
    }

    public void onInitialize(CometEvent event) throws IOException {
        System.out.println("     -> onInitialize Handler:" + hashCode());
        getResponse().addHeader(BasicCometTest.onInitialize,
            event.attachment() == null ? BasicCometTest.onInitialize : event.attachment().toString());
        onInitializeCalled.set(true);
    }

    public void onTerminate(CometEvent event) throws IOException {
        System.out.println("    -> onTerminate Handler:" + hashCode());
        onTerminateCalled.set(true);
        write(BasicCometTest.onTerminate);
    }

    public void onInterrupt(CometEvent event) throws IOException {
        System.out.println("    -> onInterrupt Handler:" + hashCode());
        onInterruptCalled.set(true);
        write(BasicCometTest.onInterrupt);
    }

    @Override
    public int compareTo(CometHandler o) {
        return hashCode() - o.hashCode();
    }
    
    private void write(String s) throws IOException {
        getResponse().getWriter().write(BasicCometTest.onInterrupt);
        
        // forcing chunking
        getResponse().getWriter().flush();
    }
}
