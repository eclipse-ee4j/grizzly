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

package org.glassfish.grizzly;

/**
 * The interface represents the result of {@link Processor} execution.
 *
 * @author Alexey Stashok
 */
public class ProcessorResult {

    private static final ProcessorResult NOT_RUN_RESULT = new ProcessorResult(Status.NOT_RUN, null);
    private static final ProcessorResult COMPLETE_RESULT = new ProcessorResult(Status.COMPLETE, null);
    private static final ProcessorResult LEAVE_RESULT = new ProcessorResult(Status.LEAVE, null);
    private static final ProcessorResult REREGISTER_RESULT = new ProcessorResult(Status.REREGISTER, null);
    private static final ProcessorResult ERROR_RESULT = new ProcessorResult(Status.ERROR, null);
    private static final ProcessorResult TERMINATE_RESULT = new ProcessorResult(Status.TERMINATE, null);

    private static ProcessorResult create() {
        return new ProcessorResult();
    }

    /**
     * Enumeration represents the status/code of {@link ProcessorResult}.
     */
    public enum Status {
        COMPLETE, LEAVE, REREGISTER, RERUN, ERROR, TERMINATE, NOT_RUN
    }

    /**
     * Result status
     */
    private Status status;
    /**
     * Result description
     */
    private Object data;

    public static ProcessorResult createComplete() {
        return COMPLETE_RESULT;
    }

    public static ProcessorResult createComplete(final Object data) {
        return create().setStatus(Status.COMPLETE).setData(data);
    }

    public static ProcessorResult createLeave() {
        return LEAVE_RESULT;
    }

    public static ProcessorResult createReregister(final Context context) {
        return create().setStatus(Status.REREGISTER).setData(context);
    }

    public static ProcessorResult createError() {
        return ERROR_RESULT;
    }

    public static ProcessorResult createError(final Object description) {
        return create().setStatus(Status.ERROR).setData(description);
    }

    public static ProcessorResult createRerun(final Context context) {
        return create().setStatus(Status.RERUN).setData(context);
    }

    public static ProcessorResult createTerminate() {
        return TERMINATE_RESULT;
    }

    public static ProcessorResult createNotRun() {
        return NOT_RUN_RESULT;
    }

    private ProcessorResult() {
        this(null, null);
    }

    private ProcessorResult(final Status status) {
        this(status, null);
    }

    private ProcessorResult(final Status status, final Object context) {
        this.status = status;
        this.data = context;
    }

    /**
     * Get the result status.
     *
     * @return the result status.
     */
    public Status getStatus() {
        return status;
    }

    protected ProcessorResult setStatus(Status status) {
        this.status = status;
        return this;
    }

    /**
     * Get the {@link ProcessorResult} extra data.
     *
     * @return the {@link ProcessorResult} extra data.
     */
    public Object getData() {
        return data;
    }

    protected ProcessorResult setData(Object context) {
        this.data = context;
        return this;
    }
}
