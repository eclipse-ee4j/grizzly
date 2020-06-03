/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 Payara Services Ltd.
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

package org.glassfish.grizzly;

import java.io.IOException;

/**
 * The {@link IOEvent} life-cycle listener, which will be notified about changes in {@link IOEvent} processing statuses.
 *
 * @author Alexey Stashok
 */
public interface IOEventLifeCycleListener {
    /**
     * {@link IOEvent} processing suspended.
     *
     * @param context IO Context
     * @throws IOException on error
     */
    void onContextSuspend(Context context) throws IOException;

    /**
     * {@link IOEvent} processing resumed.
     *
     * @param context IO Context
     * @throws IOException on error
     */
    void onContextResume(Context context) throws IOException;

    /**
     * Processing switched to the manual IOEvent control. {@link Connection#enableIOEvent(org.glassfish.grizzly.IOEvent)} or
     * {@link Connection#disableIOEvent(org.glassfish.grizzly.IOEvent)} might be explicitly called.
     *
     * @param context IO Context on error
     */
    void onContextManualIOEventControl(final Context context) throws IOException;

    /**
     * Reregister {@link IOEvent} interest.
     *
     * @param context IO Context
     * @throws IOException on error
     */
    void onReregister(Context context) throws IOException;

    /**
     * {@link IOEvent} processing completed.
     *
     * @param context IO Context
     * @param data data produced
     * @throws IOException on error
     */
    void onComplete(Context context, Object data) throws IOException;

    /**
     * Detaching {@link IOEvent} processing out of this {@link Context}.
     *
     * @param context IO Context
     * @throws IOException on error
     */
    void onLeave(Context context) throws IOException;

    /**
     * Terminate {@link IOEvent} processing in this thread, but it's going to be continued later.
     *
     * @param context IO Context
     * @throws IOException on error
     *
     * @deprecated will never be invoked
     */
    @Deprecated
    void onTerminate(Context context) throws IOException;

    /**
     * Re-run {@link IOEvent} processing.
     *
     * @param context original {@link Context} to be rerun
     * @param newContext new context, which will replace original {@link Context}
     * @throws IOException on error
     */
    void onRerun(Context context, Context newContext) throws IOException;

    /**
     * Error occurred during {@link IOEvent} processing.
     *
     * @param context IO Context
     * @param description description of error. This may be ignored.
     * @throws java.io.IOException on error
     */
    void onError(Context context, Object description) throws IOException;

    /**
     * {@link IOEvent} wasn't processed.
     *
     * @param context IO Context on error
     * @throws java.io.IOException on error
     */
    void onNotRun(Context context) throws IOException;

    /**
     * Empty {@link IOEventLifeCycleListener} implementation.
     */
    class Adapter implements IOEventLifeCycleListener {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextSuspend(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextResume(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onComplete(Context context, Object data) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTerminate(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Context context, Object description) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onNotRun(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onContextManualIOEventControl(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReregister(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onLeave(Context context) throws IOException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRerun(Context context, Context newContext) throws IOException {
        }
    }
}
