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

package org.glassfish.grizzly.nio;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * Configurator responsible for initial {@link SelectableChannel} configuration.
 * Such a configurator(s) could be used by {@link NIOTransport} to customize
 * configuration of newly created {@link NIOConnection}s.
 * Depending on {@link NIOTransport} nature, could be used both for client
 * and server side connections.
 * 
 * @author Alexey Stashok
 */
public interface ChannelConfigurator {
    /**
     * This method is called by a {@link NIOTransport} to configure newly created
     * {@link SelectableChannel} at the very early stage, right after the object
     * has been created.
     * 
     * @param transport
     * @param channel 
     * @throws java.io.IOException 
     */
    void preConfigure(NIOTransport transport, SelectableChannel channel)
            throws IOException;
    
    /**
     * This method is called by a {@link NIOTransport} to configure newly created
     * {@link SelectableChannel} once it's been connected/accepted and become ready to use.
     * 
     * @param transport
     * @param channel 
     * @throws java.io.IOException 
     */
    void postConfigure(NIOTransport transport, SelectableChannel channel)
            throws IOException;
}
