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

package org.glassfish.grizzly.utils;

import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;

import junit.framework.TestCase;

public class LogMessagesTest extends TestCase {
    public void testMessageNumbers() {
        ResourceBundle bundle = ResourceBundle.getBundle("org.glassfish.grizzly.localization.log");
        final Enumeration<String> keys = bundle.getKeys();
        Set<String> found = new TreeSet<>();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            final String value = bundle.getString(key);
            String id = value.split(":")[0];
            if (found.contains(id)) {
                Assert.fail(String.format("Duplicate ID found (%s) for key %s", id, key));
            } else {
                found.add(id);
            }
        }
    }
}
