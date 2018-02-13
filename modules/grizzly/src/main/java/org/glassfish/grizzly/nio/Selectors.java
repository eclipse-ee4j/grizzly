/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.nio;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;

/**
 * Utility class for {@link Selector} related operations.
 * @author Alexey Stashok
 */
public final class Selectors {

    /**
     * Creates new {@link Selector} using passed {@link SelectorProvider}.
     * 
     * @param provider {@link SelectorProvider}
     * @return {@link Selector}
     * @throws IOException 
     */
    public static Selector newSelector(final SelectorProvider provider)
            throws IOException {
        try {
            return provider.openSelector();
        } catch (NullPointerException e) {
            // This is a JDK issue http://bugs.glassfish.com/view_bug.do?bug_id=6427854
            // Try 5 times and abort
            for (int i = 0; i < 5; i++) {
                try {
                    return provider.openSelector();
                } catch (NullPointerException ignored) {
                }
            }

            throw new IOException("Can not open Selector due to NPE");
        }
    }
}
