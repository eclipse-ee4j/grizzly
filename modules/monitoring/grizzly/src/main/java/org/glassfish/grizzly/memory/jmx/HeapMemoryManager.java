/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.memory.jmx;

import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.InheritedAttribute;
import org.glassfish.gmbal.InheritedAttributes;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * {@link org.glassfish.grizzly.memory.HeapMemoryManager} JMX object.
 *
 * @author Alexey Stashok
 */
@ManagedObject
@Description("Grizzly Heap Memory Manager, which uses thread local memory pool")
@InheritedAttributes({@InheritedAttribute(id="is-direct", description="is-dirfff")})
public class HeapMemoryManager extends MemoryManager {

    public HeapMemoryManager(org.glassfish.grizzly.memory.HeapMemoryManager memoryManager) {
        super(memoryManager);
    }

    @ManagedAttribute(id="max-buffer-size")
    @Description("The max buffer size, which could be associated with a thread")
    public int getMaxThreadBufferSize() {
        return ((org.glassfish.grizzly.memory.AbstractMemoryManager) memoryManager).getMaxBufferSize();
    }
}
