/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Cacheable;

/**
 * Abstraction, which represents any type of HTTP message: {@link HttpRequestPacket}, {@link HttpResponsePacket},
 * {@link HttpContent}.
 *
 * @see HttpRequestPacket
 * @see HttpResponsePacket
 * @see HttpContent
 *
 * @author Alexey Stashok
 */
public abstract class HttpPacket implements Cacheable {
    /**
     * Returns <tt>true</tt> if passed {@link Object} is a <tt>HttpPacket</tt>.
     *
     * @param packet
     * @return <tt>true</tt> if passed {@link Object} is a <tt>HttpPacket</tt>.
     */
    public static boolean isHttp(final Object packet) {
        return HttpPacket.class.isAssignableFrom(packet.getClass());
    }

    /**
     * Returns <tt>true</tt>, if this HTTP message represents HTTP message header, or <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt>, if this HTTP message represents HTTP message header, or <tt>false</tt> otherwise.
     */
    public abstract boolean isHeader();

    /**
     * Get the HTTP message header, associated with this HTTP packet.
     *
     * @return {@link HttpHeader}.
     */
    public abstract HttpHeader getHttpHeader();
}
