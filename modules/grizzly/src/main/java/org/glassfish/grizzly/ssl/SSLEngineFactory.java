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

package org.glassfish.grizzly.ssl;

import javax.net.ssl.SSLEngine;

/**
 * The factory responsible for creating {@link SSLEngine}.
 * 
 * @author Grizzly team
 */
public interface SSLEngineFactory {
    /**
     * Create and configure {@link SSLEngine} using this factory configuration
     * using advisory peer information.
     * <P>
     * Applications using this factory method may provide hints
     * for an internal session reuse strategy by providing peerHost and peerPort
     * information.
     * <P>
     * Some cipher suites (such as Kerberos) require remote hostname
     * information, in which case peerHost needs to be specified.
     * 
     * @param   peerHost the non-authoritative name of the host, or <tt>null</tt> to not specify one
     * @param   peerPort the non-authoritative port, or <tt>-1</tt> to not specify one
     * 
     * @return {@link SSLEngine}.
     */

    SSLEngine createSSLEngine(String peerHost, int peerPort);
}
