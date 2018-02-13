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

package org.glassfish.grizzly.portunif;

import org.glassfish.grizzly.filterchain.FilterChainContext;

/**
 * General interface for protocol finders, responsible to figure out, whether
 * incoming bytes belong to the specific protocol.
 * 
 * @author Alexey Stashok
 */
public interface ProtocolFinder {
    enum Result {
        FOUND, NOT_FOUND, NEED_MORE_DATA
    }

    /**
     * Method is called from {@link PUFilter} to check whether the incoming
     * bytes belong to the specific protocol.
     *
     * @param puContext {@link PUContext}
     * @param ctx {@link FilterChainContext}
     * @return {@link Result}
     */
    Result find(final PUContext puContext,
                final FilterChainContext ctx);
}
