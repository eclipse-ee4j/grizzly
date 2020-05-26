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
 * Represents the result of message encoding/decoding.
 *
 * @author Alexey Stashok
 */
public class TransformationResult<I, O> implements Cacheable {
    private static final ThreadCache.CachedTypeIndex<TransformationResult> CACHE_IDX = ThreadCache.obtainIndex(TransformationResult.class, 2);

    public static <I, O> TransformationResult<I, O> createErrorResult(int errorCode, String errorDescription) {
        return create(Status.ERROR, null, null, errorCode, errorDescription);
    }

    public static <I, O> TransformationResult<I, O> createCompletedResult(O message, I externalRemainder) {
        return create(Status.COMPLETE, message, externalRemainder, 0, null);
    }

    public static <I, O> TransformationResult<I, O> createIncompletedResult(I externalRemainder) {
        return create(Status.INCOMPLETE, null, externalRemainder, 0, null);
    }

    @SuppressWarnings("unchecked")
    private static <I, O> TransformationResult<I, O> create(Status status, O message, I externalRemainder, int errorCode, String errorDescription) {

        final TransformationResult<I, O> result = ThreadCache.takeFromCache(CACHE_IDX);
        if (result != null) {
            result.setStatus(status);
            result.setMessage(message);
            result.setExternalRemainder(externalRemainder);
            result.setErrorCode(errorCode);
            result.setErrorDescription(errorDescription);

            return result;
        }

        return new TransformationResult<>(status, message, externalRemainder, errorCode, errorDescription);
    }

    public enum Status {
        COMPLETE, INCOMPLETE, ERROR
    }

    private O message;
    private Status status;

    private int errorCode;
    private String errorDescription;

    private I externalRemainder;

    public TransformationResult() {
        this(Status.COMPLETE, null, null);
    }

    public TransformationResult(Status status, O message, I externalRemainder) {
        this.status = status;
        this.message = message;
        this.externalRemainder = externalRemainder;
    }

    /**
     * Creates error transformation result with specific code and description.
     *
     * @param errorCode id of the error
     * @param errorDescription error description
     */
    public TransformationResult(int errorCode, String errorDescription) {
        this.status = Status.ERROR;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    protected TransformationResult(Status status, O message, I externalRemainder, int errorCode, String errorDescription) {
        this.status = status;
        this.message = message;
        this.externalRemainder = externalRemainder;

        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public O getMessage() {
        return message;
    }

    public void setMessage(O message) {
        this.message = message;
    }

    public I getExternalRemainder() {
        return externalRemainder;
    }

    public void setExternalRemainder(I externalRemainder) {
        this.externalRemainder = externalRemainder;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("Transformation result. Status: ").append(status);
        sb.append(" message: ").append(message);

        if (status == Status.ERROR) {
            sb.append(" errorCode: ").append(errorCode);
            sb.append(" errorDescription: ").append(errorDescription);
        }

        return sb.toString();
    }

    /**
     * If implementation uses {@link org.glassfish.grizzly.utils.ObjectPool} to store and reuse {@link TransformationResult}
     * instances - this method will be called before {@link TransformationResult} will be offered to pool.
     */
    public void reset() {
        message = null;
        status = null;

        errorCode = 0;
        errorDescription = null;
        externalRemainder = null;
    }

    /**
     * Recycle this {@link Context}
     */
    @Override
    public void recycle() {
        reset();
        ThreadCache.putToCache(CACHE_IDX, this);
    }
}
