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

package org.glassfish.grizzly.osgi.httpservice;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test for {@link OSGiCleanMapper}.
 *
 * @author Hubert Iwaniuk
 */
public class OSGiCleanMapperTest {

    @Test public void testMap_simpleContext() {
        OSGiCleanMapper mapper = new OSGiCleanMapper(null);
        mapper.addHttpHandler("/testAlias", null);
        assertEquals("Registered alias should be found.", "/testAlias", OSGiCleanMapper.map("/testAlias", false));
    }

    @Test public void testMap_simpleContextSub() {
        OSGiCleanMapper mapper = new OSGiCleanMapper(null);
        mapper.addHttpHandler("/a", null);
        assertEquals("Registered alias should be found.", "/a", OSGiCleanMapper.map("/a/index.html", true));
    }

    @Test public void testMap_notRegistered() {
        OSGiCleanMapper mapper = new OSGiCleanMapper(null);
        mapper.addHttpHandler("/testAlias", null);
        assertNull("Should not be able to map not registered resource.", OSGiCleanMapper.map("/notregistered", true));
        assertNull("Should not be able to map not registered resource.", OSGiCleanMapper.map("/notregistered", false));
    }

    @Test public void testMap_complexContext() {
        OSGiCleanMapper mapper = new OSGiCleanMapper(null);
        mapper.addHttpHandler("/a", null);
        mapper.addHttpHandler("/a/b", null);
        assertNull("Should not be able to map not registered resource.", OSGiCleanMapper.map("/a/b/c", false));
        assertNull("Should not be able to map not registered resource.", OSGiCleanMapper.map("/a/b/", false));
        assertEquals("Registered alias should be found.", "/a/b", OSGiCleanMapper.map("/a/b/c", true));
        assertEquals("Registered alias should be found.", "/a/b", OSGiCleanMapper.map("/a/b/", true));
        assertEquals("Registered alias should be found.", "/a", OSGiCleanMapper.map("/a/b", true));
        assertEquals("Registered alias should be found.", "/a/b", OSGiCleanMapper.map("/a/b", false));
        assertNull("Should not be able to map not registered resource.", OSGiCleanMapper.map("/a", true));
    }
}
