/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.http.util.ContentType;
import org.glassfish.grizzly.utils.Charsets;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test {@link ContentType}
 */
public class ContentTypeTest {
    @Test
    public void testContentType() throws Exception {
        final ContentType.SettableContentType ct =
                ContentType.newSettableContentType();
        
        ct.set("text/plain");
        assertEquals("text/plain", ct.getMimeType());
        assertNull(ct.getCharacterEncoding());

        assertEquals("text/plain",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        ct.reset();
        
        ct.set("text/plain;charset=UTF-8");
        assertEquals("text/plain", ct.getMimeType());
        assertEquals("UTF-8", ct.getCharacterEncoding());
        
        assertEquals("text/plain;charset=UTF-8",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        ct.reset();

        ct.set("text/plain;charset=UTF-8;abc=xyz");
        assertEquals("text/plain;abc=xyz", ct.getMimeType());
        assertEquals("UTF-8", ct.getCharacterEncoding());
        
        assertEquals("Incorrect value=" + new String(ct.getByteArray(), 0,
                                                     ct.getArrayLen(),
                                                     Charsets.ASCII_CHARSET),
                     "text/plain;charset=UTF-8;abc=xyz",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        
        ct.setMimeType("text/html");
        assertEquals("text/html", ct.getMimeType());
        assertEquals("UTF-8", ct.getCharacterEncoding());
        
        assertEquals("text/html;charset=UTF-8",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));

        ct.setCharacterEncoding("UTF-16");
        assertEquals("text/html", ct.getMimeType());
        assertEquals("UTF-16", ct.getCharacterEncoding());
        
        assertEquals("text/html;charset=UTF-16",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
    
        ct.setCharacterEncoding(null);
        assertEquals("text/html", ct.getMimeType());
        assertEquals(null, ct.getCharacterEncoding());
        
        assertEquals("text/html",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));

        ct.reset();

        ct.set("text/html;charset=Shift_Jis");
        ct.set("text/xml");
        assertEquals("text/xml;charset=Shift_Jis", ct.get());
        assertNotNull(ct.getCharacterEncoding());
        
        ct.reset();
        
        ContentType prepared = ContentType.newContentType("application/json;charset=UTF-8").prepare();
        ct.set(prepared);
        
        assertEquals("application/json;charset=UTF-8",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        assertEquals("application/json", ct.getMimeType());
        assertEquals("UTF-8", ct.getCharacterEncoding());
        
        prepared = ContentType.newContentType("text/plain", "UTF-16");
        ct.set(prepared);
        
        assertEquals("text/plain;charset=UTF-16",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        assertEquals("text/plain", ct.getMimeType());
        assertEquals("UTF-16", ct.getCharacterEncoding());

        ct.reset();
        
        final String longCt = "text/plain;aaa=aaa1;bbb=bbb1;charset=UTF-8;ccc=ccc1;ddd=ddd1;eee=eee1;fff=fff1";
        final String longMt = longCt.replace("charset=UTF-8;", "");
        
        // test long content-type
        ct.set(longCt);
        
        assertEquals(longCt,
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        assertEquals(longMt, ct.getMimeType());
        assertEquals("UTF-8", ct.getCharacterEncoding());
        
        ct.reset();

        // test long content-type
        ct.setCharacterEncoding("charset=Shift_Jis");
        ct.set(longCt);
        
        assertEquals(longMt + ";charset=UTF-8",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        assertEquals(longMt, ct.getMimeType());
        assertEquals("UTF-8", ct.getCharacterEncoding());

        ct.reset();
        
        ct.setMimeType(longMt);
        ct.setCharacterEncoding("UTF-16");
        
        assertEquals(longMt + ";charset=UTF-16",
                     new String(ct.getByteArray(), 0, ct.getArrayLen(),
                                Charsets.ASCII_CHARSET));
        assertEquals(longMt, ct.getMimeType());
        assertEquals("UTF-16", ct.getCharacterEncoding());
        
    }
}
