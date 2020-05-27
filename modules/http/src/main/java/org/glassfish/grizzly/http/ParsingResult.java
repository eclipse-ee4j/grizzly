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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Cacheable;
import org.glassfish.grizzly.ThreadCache;

/**
 * Class, which represents result of {@link TransferEncoding} or {@link ContentEncoding} parsing.
 *
 * @author Alexey Stashok
 */
public final class ParsingResult implements Cacheable {

    private static final ThreadCache.CachedTypeIndex<ParsingResult> CACHE_IDX = ThreadCache.obtainIndex(ParsingResult.class, 1);
    private HttpContent httpContent;
    private Buffer remainderBuffer;
    private boolean sendHeaderUpstream = true;

    public static ParsingResult create(final HttpContent httpContent, final Buffer remainderBuffer) {
        ParsingResult resultObject = ThreadCache.takeFromCache(CACHE_IDX);
        if (resultObject == null) {
            resultObject = new ParsingResult();
        }
        resultObject.httpContent = httpContent;
        resultObject.remainderBuffer = remainderBuffer;
        return resultObject;
    }

    public static ParsingResult create(final HttpContent httpContent, final Buffer remainderBuffer, final boolean sendHeaderUpstream) {
        ParsingResult resultObject = create(httpContent, remainderBuffer);
        resultObject.sendHeaderUpstream = sendHeaderUpstream;
        return resultObject;
    }

    private ParsingResult() {
    }

    public Buffer getRemainderBuffer() {
        return remainderBuffer;
    }

    public HttpContent getHttpContent() {
        return httpContent;
    }

    public boolean isSendHeaderUpstream() {
        return sendHeaderUpstream;
    }

    @Override
    public void recycle() {
        remainderBuffer = null;
        httpContent = null;
        sendHeaderUpstream = true;

        ThreadCache.putToCache(CACHE_IDX, this);
    }
}
