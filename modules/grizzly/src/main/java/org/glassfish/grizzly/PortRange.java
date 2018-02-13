/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable class representing a port range.
 * @author Gerd Behrmann
 * @author Tigran Mkrtchyan
 */
public class PortRange {

    /** Pattern matching <PORT>[:<PORT>] */
    private final static Pattern FORMAT =
            Pattern.compile("(\\d+)(?:(?:,|:)(\\d+))?");
    private final int lower;
    private final int upper;

    /**
     * Creates a port range with the given bounds (both inclusive).
     *
     * @throws IllegalArgumentException is either bound is not between
     *         1 and 65535, or if <code>high</code> is lower than
     *         <code>low</code>.
     */
    public PortRange(final int low, final int high) {
        if (low < 1 || high < low || 65535 < high) {
            throw new IllegalArgumentException("Invalid range");
        }
        lower = low;
        upper = high;
    }

    /**
     * Creates a port range containing a single port.
     */
    public PortRange(final int port) {
        this(port, port);
    }

    /**
     * Parse a port range. A port range consists of either a single
     * integer, or two integers separated by either a comma or a
     * colon.
     *
     * The bounds must be between 1 and 65535, both inclusive.
     *
     * @return The port range represented by <code>s</code>.
     */
    public static PortRange valueOf(String s)
            throws IllegalArgumentException {
        Matcher m = FORMAT.matcher(s);

        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid string format: " + s);
        }
        
        int low;
        int high;
        
        try {
            low = Integer.parseInt(m.group(1));
            high = (m.groupCount() == 1) ? low : Integer.parseInt(m.group(2));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid string format: " + s);
        }

        return new PortRange(low, high);
    }

    public int getLower() {
        return lower;
    }

    public int getUpper() {
        return upper;
    }

    @Override
    public String toString() {
        return String.format("%d:%d", lower, upper);
    }
}
