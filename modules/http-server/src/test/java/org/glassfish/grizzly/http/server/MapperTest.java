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

package org.glassfish.grizzly.http.server;

import org.glassfish.grizzly.http.server.util.Mapper;
import org.glassfish.grizzly.http.server.util.MappingData;
import org.glassfish.grizzly.http.util.DataChunk;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * {@link Mapper} tests
 */
public class MapperTest {
    @Test
    public void testVirtualServer() throws Exception {
        final Object defaultHost = new Object();
        final Object host1 = new Object();
        final Mapper mapper = new Mapper();
        mapper.setDefaultHostName("default");
        mapper.addHost("default", new String[] {"default"}, defaultHost);
        mapper.addHost("host1", new String[] {"host1"}, host1);
        
        final Object context1 = new Object();
        mapper.addContext("default", "/context1", context1, null, null);
        mapper.addContext("host1", "/context1", context1, null, null);
        
        final Object wrapper11 = new Object();
        final Object wrapper12 = new Object();
        mapper.addWrapper("default", "/context1", "/wrapper11", wrapper11);
        mapper.addWrapper("host1", "/context1", "/wrapper11", wrapper11);
        mapper.addWrapper("default", "/context1", "/wrapper12", wrapper12);
        mapper.addWrapper("host1", "/context1", "/wrapper12", wrapper12);
        
        final Object context2 = new Object();
        mapper.addContext("host1", "/context2", context2, null, null);
        
        final Object wrapper21 = new Object();
        mapper.addWrapper("host1", "/context2", "/wrapper21", wrapper21);
        
        
        // Test wrapper1 on default host
        final DataChunk host = DataChunk.newInstance();
        host.setBytes("default".getBytes());
        
        final DataChunk uri = DataChunk.newInstance();
        uri.setBytes("/context1/wrapper11".getBytes());
        
        MappingData md = new MappingData();
        mapper.map(host, uri, md);
        
        assertEquals(defaultHost, md.host);
        assertEquals(context1, md.context);
        assertEquals(wrapper11, md.wrapper);

        
        // Test no wrapper2 on default host
        md.recycle();
        uri.recycle();

        uri.setBytes("/context2/wrapper21".getBytes());
        
        mapper.map(host, uri, md);
        
        assertEquals(defaultHost, md.host);
        assertNull(md.context);
        assertNull(md.wrapper);

        // Test wrapper2 on host1
        md.recycle();
        host.recycle();
        uri.recycle();

        host.setBytes("host1".getBytes());
        uri.setBytes("/context2/wrapper21".getBytes());
        
        mapper.map(host, uri, md);
        
        assertEquals(host1, md.host);
        assertEquals(context2, md.context);
        assertEquals(wrapper21, md.wrapper);
        
    }
}
