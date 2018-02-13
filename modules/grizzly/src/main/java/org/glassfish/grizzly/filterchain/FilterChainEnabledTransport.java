/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.filterchain;

/**
 * Common interface for Transports, which support {@link FilterChain}s.
 *
 * @see org.glassfish.grizzly.Transport
 * @see FilterChain
 * 
 * @author Alexey Stashok
 */
public interface FilterChainEnabledTransport {
    /**
     * Get transport {@link Filter}, which is aware of {@link org.glassfish.grizzly.Transport}
     * specifics; knows how to read/write from/to {@link org.glassfish.grizzly.Transport}
     * specific {@link org.glassfish.grizzly.Connection} streams.
     * 
     * Each {@link org.glassfish.grizzly.Transport} should provide transport {@link Filter}
     * implementation.
     *
     * @return transport {@link Filter}, which is aware of {@link org.glassfish.grizzly.Transport}
     * specifics; knows how to read/write from/to {@link org.glassfish.grizzly.Transport}
     * specific {@link org.glassfish.grizzly.Connection}s.
     */
    Filter getTransportFilter();
}
