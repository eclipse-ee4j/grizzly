/*
 * Copyright (c) 2014, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import java.io.IOException;

import org.glassfish.grizzly.http2.frames.ErrorCode;

/**
 * HTTP/2 session exception.
 *
 * Unlike {@link Http2StreamException}, this exception means severe problem related to the entire HTTP2 session.
 *
 * @author Alexey Stashok
 */
public final class Http2SessionException extends IOException {
    private final ErrorCode errorCode;

    /**
     * Construct <tt>Http2SessionException</tt>.
     *
     * @param errorCode the {@link ErrorCode} for this {@link Http2SessionException}
     */
    public Http2SessionException(final ErrorCode errorCode) {
        this(errorCode, null);
    }

    /**
     * Construct <tt>Http2SessionException</tt>.
     *
     * @param errorCode the {@link ErrorCode} for this {@link Http2SessionException}
     * @param message the detail message for this {@link Http2SessionException}
     */
    public Http2SessionException(final ErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * @return the {@link ErrorCode} reason to be sent with a GoAway.
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(128);
        sb.append(getClass().getName()).append(" errorCode=").append(errorCode);

        String message = getLocalizedMessage();

        return message != null ? sb.append(": ").append(message).toString() : sb.toString();
    }
}
