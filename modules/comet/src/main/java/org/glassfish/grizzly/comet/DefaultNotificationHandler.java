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
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default NotificationHandler that uses a thread pool dedicated to the CometEngine to execute the notification
 * process.<br>
 *
 * @author Jeanfrancois Arcand
 * @author Gustav Trede
 */
public class DefaultNotificationHandler implements NotificationHandler {
    private final static Logger logger = Logger.getLogger(DefaultNotificationHandler.class.getName());
    private static final IllegalStateException ISEempty = new IllegalStateException();
    /**
     * The {@link ExecutorService} used to execute threaded notification.
     */
    protected ExecutorService threadPool;

    public DefaultNotificationHandler() {
    }

    /**
     * Set the {@link ExecutorService} used for notifying the CometHandler.
     */
    protected void setThreadPool(ExecutorService threadPool) {
        this.threadPool = threadPool;
    }

    /**
     * Notify all {@link CometHandler}.
     *
     * @param cometEvent the CometEvent used to notify CometHandler
     * @param iteratorHandlers An iterator over a list of CometHandler
     */
    @Override
    public void notify(final CometEvent cometEvent, final Iterator<CometHandler> iteratorHandlers) throws IOException {
        while (iteratorHandlers.hasNext()) {
            notify(cometEvent, iteratorHandlers.next());
        }
    }

    /**
     * Notify the {@link CometHandler}.
     *
     * @param cometEvent cometEvent the CometEvent used to notify CometHandler
     */
    @Override
    public void notify(final CometEvent cometEvent, final CometHandler cometHandler) throws IOException {
        notify0(cometEvent, cometHandler);
    }

    /**
     * Notify a {@link CometHandler}.
     * <p/>
     * CometEvent.INTERRUPT -> <code>CometHandler.onInterrupt</code> CometEvent.NOTIFY -> <code>CometHandler.onEvent</code>
     * CometEvent.INITIALIZE -> <code>CometHandler.onInitialize</code> CometEvent.TERMINATE ->
     * <code>CometHandler.onTerminate</code> CometEvent.READ -> <code>CometHandler.onEvent</code> CometEvent.WRITE ->
     * <code>CometHandler.onEvent</code>
     *
     * @param cometEvent An object shared amongst {@link CometHandler}.
     * @param cometHandler The CometHandler to invoke.
     */
    protected void notify0(CometEvent cometEvent, CometHandler cometHandler) {
        try {
            switch (cometEvent.getType()) {
            case INTERRUPT:
                cometHandler.onInterrupt(cometEvent);
                break;
            case NOTIFY:
            case READ:
            case WRITE:
                if (cometEvent.getCometContext().isActive(cometHandler)) {
                    cometHandler.onEvent(cometEvent);
                }
                break;
            case INITIALIZE:
                cometHandler.onInitialize(cometEvent);
                break;
            case TERMINATE:
                cometHandler.onTerminate(cometEvent);
                break;
            default:
                throw ISEempty;
            }
        } catch (Throwable ex) {
            logger.log(Level.FINE, "Notification failed: ", ex);
            try {
                cometEvent.getCometContext().resumeCometHandler(cometHandler);
            } catch (Throwable t) {
                logger.log(Level.FINE, "Resume phase failed: ", t);
            }
        }
    }

}
