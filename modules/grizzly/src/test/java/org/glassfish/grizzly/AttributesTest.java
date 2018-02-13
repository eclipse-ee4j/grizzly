/*
 * Copyright (c) 2014, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.attributes.DefaultAttributeBuilder;
import org.glassfish.grizzly.utils.NullaryFunction;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Testing {@link Attribute}s.
 * 
 * @author Alexey Stashok
 */
@RunWith(Parameterized.class)
public class AttributesTest {
    
    @Parameterized.Parameters
    public static Collection<Object[]> isSafe() {
        return Arrays.asList(new Object[][]{
                    {Boolean.FALSE},
                    {Boolean.TRUE}
                });
    }
    
    private final boolean isSafe;
    
    public AttributesTest(final boolean isSafe) {
        this.isSafe = isSafe;
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAttributes() {
        AttributeBuilder builder = new DefaultAttributeBuilder();
        AttributeHolder holder = isSafe
                ? builder.createSafeAttributeHolder()
                : builder.createUnsafeAttributeHolder();
        
        final int attrCount = 10;
        
        final Attribute[] attrs = new Attribute[attrCount];
        
        for (int i = 0; i < attrCount; i++) {
            attrs[i] = builder.createAttribute("attribute-" + i);
        }
        
        // set values
        for (int i = 0; i < attrCount; i++) {
            attrs[i].set(holder, "value-" + i);
        }
        
        // check values
        for (int i = 0; i < attrCount; i++) {
            assertTrue(attrs[i].isSet(holder));
            assertEquals("value-" + i, attrs[i].get(holder));
        }
        
        assertNotNull(attrs[0].remove(holder));
        assertFalse(attrs[0].isSet(holder));
        assertNull(attrs[0].remove(holder));
        assertNull(attrs[0].get(holder));
        
        assertNotNull(attrs[attrCount - 1].remove(holder));
        assertFalse(attrs[attrCount - 1].isSet(holder));
        assertNull(attrs[attrCount - 1].remove(holder));
        assertNull(attrs[attrCount - 1].get(holder));
        
        
        final Set<String> attrNames = holder.getAttributeNames();
        assertEquals(attrCount - 2, attrNames.size());
        
        for (int i = 1; i < attrCount - 1; i++) {
            assertTrue(attrNames.contains(attrs[i].name()));
        }
    }

    @Test
    public void testAttributeGetWithNullaryFunctionOnEmptyHolder() {
        AttributeBuilder builder = new DefaultAttributeBuilder();
        AttributeHolder holder = isSafe
                ? builder.createSafeAttributeHolder()
                : builder.createUnsafeAttributeHolder();

        final Attribute<String> attr = builder.createAttribute(
                "attribute",
                new NullaryFunction<String>() {
                    @Override
                    public String evaluate() {
                        return "default";
                    }
                }
        );

        assertNull(attr.peek(holder));
        assertEquals("default", attr.get(holder));
        assertTrue(attr.isSet(holder));
        assertEquals("default", attr.peek(holder));
    }

    @Test
    public void testAttributeGetWithoutInitializerOnEmptyHolder() {
        AttributeBuilder builder = new DefaultAttributeBuilder();
        AttributeHolder holder = isSafe
                ? builder.createSafeAttributeHolder()
                : builder.createUnsafeAttributeHolder();

        final Attribute<String> attr = builder.createAttribute("attribute");

        assertNull(attr.peek(holder));
        assertEquals(null, attr.get(holder));
        assertFalse(attr.isSet(holder));
    }
}
