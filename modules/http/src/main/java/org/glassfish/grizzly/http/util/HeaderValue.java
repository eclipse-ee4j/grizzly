/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.util;

import static org.glassfish.grizzly.http.util.HttpCodecUtils.EMPTY_ARRAY;
import static org.glassfish.grizzly.http.util.HttpCodecUtils.toCheckedByteArray;

/**
 * This class serves as an HTTP header value holder, plus it implements useful utility methods to optimize headers
 * serialization.
 *
 * @author Alexey Stashok
 */
public final class HeaderValue {
    public static final HeaderValue IDENTITY = newHeaderValue("identity").prepare();

    private final String value;
    private byte[] preparedByteArray;

    /**
     * Creates a {@link HeaderValue} wrapper over a {@link String} header value representation.
     *
     * @param value {@link String} header value representation
     * @return a {@link HeaderValue} wrapper over a {@link String} heade value representation
     */
    public static HeaderValue newHeaderValue(final String value) {
        return new HeaderValue(value);
    }

    private HeaderValue(final String value) {
        this.value = value;
    }

    /**
     * Prepare the <tt>HeaderValue</tt> for the serialization.
     *
     * This method might be particularly useful if we use the same <tt>HeaderValue</tt> over and over for different
     * responses, so that the <tt>HeaderValue</tt> will not have to be parsed and prepared for each response separately.
     *
     * @return this <tt>HeaderValue</tt>
     */
    public HeaderValue prepare() {
        if (preparedByteArray == null) {
            getByteArray();
        }

        return this;
    }

    /**
     * @return <tt>true</tt> if header value is not null, or <tt>false</tt> otherwise
     */
    public boolean isSet() {
        return value != null;
    }

    /**
     * @return the header value string
     */
    public String get() {
        return value;
    }

    /**
     * @return the byte array representation of the header value
     */
    public byte[] getByteArray() {
        // if there's prepared byte array - return it
        if (preparedByteArray != null) {
            return preparedByteArray;
        }

        if (value == null) {
            return EMPTY_ARRAY;
        }

        preparedByteArray = toCheckedByteArray(value);
        return preparedByteArray;
    }

    @Override
    public String toString() {
        return value;
    }

    /**
     * Serializes this <tt>HeaderValue</tt> value into a passed {@link DataChunk}.
     *
     * @param dc {@link DataChunk}
     */
    public void serializeToDataChunk(final DataChunk dc) {
        if (preparedByteArray != null) {
            dc.setBytes(preparedByteArray);
        } else {
            dc.setString(value);
        }
    }
}
