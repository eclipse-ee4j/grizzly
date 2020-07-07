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

package org.glassfish.grizzly.http.server;

import org.glassfish.grizzly.http.CompressionConfig.CompressionMode;
import org.glassfish.grizzly.http.CompressionConfig.CompressionModeI;

/**
 * @deprecated pls. use {@link org.glassfish.grizzly.http.Compression#CompressionLevel}.
 */
public enum CompressionLevel implements CompressionModeI {
    OFF(CompressionMode.OFF), ON(CompressionMode.ON), FORCE(CompressionMode.FORCE);

    private final CompressionMode normalizedLevel;

    CompressionLevel(final CompressionMode normalizedLevel) {
        this.normalizedLevel = normalizedLevel;
    }

    public CompressionMode normalize() {
        return normalizedLevel;
    }

    /**
     * Set compression level.
     */
    public static CompressionLevel getCompressionLevel(String compression) {
        if ("on".equalsIgnoreCase(compression)) {
            return CompressionLevel.ON;
        } else if ("force".equalsIgnoreCase(compression)) {
            return CompressionLevel.FORCE;
        } else if ("off".equalsIgnoreCase(compression)) {
            return CompressionLevel.OFF;
        }
        throw new IllegalArgumentException();
    }
}
