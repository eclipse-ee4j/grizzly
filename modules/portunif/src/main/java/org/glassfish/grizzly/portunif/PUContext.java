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

package org.glassfish.grizzly.portunif;

/**
 * Protocol discovering context, passed to the {@link ProtocolFinder}.
 *
 * @author Alexey Stashok
 */
public class PUContext {

    private int protocolMissCount;

    short skippedProtocolFinders;
    boolean isSticky = true;
    PUProtocol protocol;

    // ------------------------------------------------------------ Constructors

    public PUContext(final PUFilter filter) {
        protocolMissCount = filter.getProtocols().size();
    }

    public boolean isSticky() {
        return isSticky;
    }

    public void setSticky(final boolean isSticky) {
        this.isSticky = isSticky;
    }

    public boolean noProtocolsFound() {
        return Integer.bitCount(skippedProtocolFinders) == protocolMissCount;
    }

    void reset() {
        isSticky = true;
        protocol = null;
        skippedProtocolFinders = 0;
        protocolMissCount = 0;
    }
}
