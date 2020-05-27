/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http;

/**
 * {@link RuntimeException}, means something wrong happened during HTTP message content parsing. Usually it happens when
 * processing HTTP message payload using {@link TransferEncoding} or {@link ContentEncoding}.
 *
 * @author Alexey Stashok
 */
public final class HttpBrokenContentException extends RuntimeException {

    public HttpBrokenContentException() {
    }

    public HttpBrokenContentException(final String message) {
        super(message);
    }

    public HttpBrokenContentException(final Throwable cause) {
        this(cause.getMessage(), cause);
    }

    public HttpBrokenContentException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
