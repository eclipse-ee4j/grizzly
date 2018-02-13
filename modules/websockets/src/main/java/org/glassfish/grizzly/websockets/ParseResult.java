/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.ThreadCache;

/**
 * {@link DataFrame} parse result.
 *
 * @author Alexey Stashok
 */
public class ParseResult implements Cacheable {
    // thread-local object cache
    private static final ThreadCache.CachedTypeIndex<ParseResult> CACHE_IDX =
        ThreadCache.obtainIndex(ParseResult.class, 1);
    // is complete
    private boolean isComplete;
    // remainder buffer (might not be null only if parsing was completed).
    private Buffer remainder;

    /**
     * Create a ParseResult object.
     *
     * @param isComplete was parsing completed?
     * @param remainderBuffer the remainder.
     *
     * @return <tt>ParseResult</tt>
     */
    public static ParseResult create(boolean isComplete, Buffer remainderBuffer) {
        ParseResult resultObject = ThreadCache.takeFromCache(CACHE_IDX);
        if (resultObject == null) {
            resultObject = new ParseResult();
        }
        resultObject.isComplete = isComplete;
        resultObject.remainder = remainderBuffer;
        return resultObject;
    }

    private ParseResult() {
    }

    /**
     * Get the parsing remainder {@link Buffer}. May not be null only in case, when parsing was completed, but some data
     * is still ready for parsing.
     *
     * @return the parsing remainder {@link Buffer}. May not be null only in case, when parsing was completed, but some
     *         data is still ready for parsing.
     */
    public Buffer getRemainder() {
        return remainder;
    }

    /**
     * Returns <tt>true</tt>, if parsing was completed, or <tt>false</tt> if more data is expected.
     *
     * @return <tt>true</tt>, if parsing was completed, or <tt>false</tt> if more data is expected.
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Recycle the object.
     */
    @Override
    public void recycle() {
        remainder = null;
        isComplete = false;
        ThreadCache.putToCache(CACHE_IDX, this);
    }
}
