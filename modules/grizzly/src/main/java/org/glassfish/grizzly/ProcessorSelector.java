/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Implementations of the interface will be responsible to find correct
 * {@link Processor}, which will process {@link IOEvent}, occurred on the
 * {@link Connection}
 * 
 * @author Alexey Stashok
 */
public interface ProcessorSelector {    
    /**
     * Selects {@link Processor}, which will process connection event.
     * 
     * @param ioEvent connection event to be processed
     * @param connection where event occurred
     * 
     * @return the {@link Processor}, which will process connection event.
     */
    Processor select(IOEvent ioEvent, Connection connection);
}
