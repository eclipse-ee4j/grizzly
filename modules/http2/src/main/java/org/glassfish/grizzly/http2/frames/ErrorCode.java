/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2.frames;

/**
 * Error codes enum
 */
public enum ErrorCode {
    NO_ERROR(0x0), PROTOCOL_ERROR(0x1), INTERNAL_ERROR(0x2), FLOW_CONTROL_ERROR(0x3), SETTINGS_TIMEOUT(0x4), STREAM_CLOSED(0x5), FRAME_SIZE_ERROR(0x6),
    REFUSED_STREAM(0x7), CANCEL(0x8), COMPRESSION_ERROR(0x9), CONNECT_ERROR(0xa), ENHANCE_YOUR_CALM(0xb), INADEQUATE_SECURITY(0xc), HTTP_1_1_REQUIRED(0xd);

    final int code;

    private static final ErrorCode[] intToErrorCode = new ErrorCode[ErrorCode.values().length];

    static {
        for (ErrorCode errorCode : ErrorCode.values()) {
            intToErrorCode[errorCode.getCode()] = errorCode;
        }
    }

    public static ErrorCode lookup(final int code) {
        return code >= 0 && code < intToErrorCode.length ? intToErrorCode[code] : INTERNAL_ERROR;
    }

    ErrorCode(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
