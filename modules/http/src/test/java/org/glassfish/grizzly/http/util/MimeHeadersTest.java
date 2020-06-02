/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MimeHeadersTest {

    MimeHeaders mimeHeaders;

    @Before
    public void setUp() {
        mimeHeaders = new MimeHeaders();
        mimeHeaders.addValue("custom-before").setString("one");
        mimeHeaders.addValue("custom-before").setString("two");
        mimeHeaders.addValue("custom-before").setString("three");
        mimeHeaders.setValue("custom-before-2").setString("one");
        mimeHeaders.mark();
        mimeHeaders.addValue("custom-after").setString("one");
        mimeHeaders.addValue("custom-after").setString("two");
        mimeHeaders.addValue("custom-after").setString("three");
        mimeHeaders.setValue("custom-after-2").setString("one");
    }

    @Test
    public void testNames() throws Exception {
        final String[] expectedNames = { "custom-before", "custom-before-2", "custom-after", "custom-after-2" };
        Iterable<String> result = mimeHeaders.names();
        List<String> list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedNames, list.toArray(new String[list.size()]));
    }

    @Test
    public void testTrailerNames() throws Exception {
        final String[] expectedNames = { "custom-after", "custom-after-2" };
        Iterable<String> result = mimeHeaders.trailerNames();
        List<String> list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedNames, list.toArray(new String[list.size()]));
    }

    @Test
    public void testValues() throws Exception {
        final String[] expectedValuesSet1 = { "one", "two", "three" };
        final String[] expectedValuesSet2 = { "one" };
        Iterable<String> result = mimeHeaders.values("custom-before");
        List<String> list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedValuesSet1, list.toArray(new String[list.size()]));

        result = mimeHeaders.values("custom-before-2");
        list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedValuesSet2, list.toArray(new String[list.size()]));

        result = mimeHeaders.values("custom-after");
        list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedValuesSet1, list.toArray(new String[list.size()]));

        result = mimeHeaders.values("custom-after-2");
        list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedValuesSet2, list.toArray(new String[list.size()]));
    }

    @Test
    public void testTrailerValues() throws Exception {
        final String[] expectedValuesSet1 = { "one", "two", "three" };
        final String[] expectedValuesSet2 = { "one" };
        final String[] emptySet = {};

        Iterable<String> result = mimeHeaders.trailerValues("custom-before");
        List<String> list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(emptySet, list.toArray(new String[list.size()]));

        result = mimeHeaders.trailerValues("custom-before-2");
        list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(emptySet, list.toArray(new String[list.size()]));

        result = mimeHeaders.trailerValues("custom-after");
        list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedValuesSet1, list.toArray(new String[list.size()]));

        result = mimeHeaders.trailerValues("custom-after-2");
        list = new ArrayList<>();
        for (String s : result) {
            list.add(s);
        }
        Assert.assertArrayEquals(expectedValuesSet2, list.toArray(new String[list.size()]));
    }

}
