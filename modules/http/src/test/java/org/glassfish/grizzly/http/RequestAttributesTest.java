/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;
import junit.framework.TestCase;

/**
 * Test the {@link HttpRequestPacket} attributes.
 * 
 * @author Alexey Stashok
 */
public class RequestAttributesTest extends TestCase {

    public void testAttributes() throws Exception {
        final String attrName = "testAttr";
        final String value1 = "value1";
        final String value2 = "value2";
        
        HttpRequestPacket packet = HttpRequestPacket.builder().build();

        assertNull(packet.getAttribute(attrName));
        
        packet.setAttribute(attrName, value1);
        assertEquals(value1, packet.getAttribute(attrName));
        
        packet.setAttribute(attrName, value2);
        assertEquals(value2, packet.getAttribute(attrName));
        
        final Set<String> attributeNames = packet.getAttributeNames();
        assertTrue(attributeNames.contains(attrName));
        try {
            attributeNames.remove(attrName);
            fail("we shouldn't be able to remove the attribute name from the set");
        } catch (UnsupportedOperationException e) {
            // readonly set, so this is expected
        }
        
        packet.removeAttribute(attrName);
        assertNull(packet.getAttribute(attrName));        
    }
    
    public void testReadOnlyAttributes() throws Exception {
        final String attrName =
                HttpRequestPacket.READ_ONLY_ATTR_PREFIX + "testAttr";
        final String originalValue = "original value";
        
        HttpRequestPacket packet = HttpRequestPacket.builder().build();

        assertNull(packet.getAttribute(attrName));
        
        packet.setAttribute(attrName, originalValue);
        assertEquals(originalValue, packet.getAttribute(attrName));
        
        packet.setAttribute(attrName, "another value");
        assertEquals(originalValue, packet.getAttribute(attrName));
        
        packet.removeAttribute(attrName);
        assertEquals(originalValue, packet.getAttribute(attrName));
        
        // the service/readonly attr shouldn't be returned in getAttributeNames()
        assertFalse(packet.getAttributeNames().contains(attrName));
    }
}
