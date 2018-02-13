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
 * {@link ProcessorSelector}, which doesn't add any {@link Processor} to process
 * occurred {@link IOEvent}.
 * {@link Connection} I/O events should be processed explicitly by calling
 * read/write/accept/connect methods.
 *
 * Setting {@link StandaloneProcessorSelector} has the same effect as setting
 * {@link StandaloneProcessor}, though if {@link StandaloneProcessorSelector} is
 * set - there is still possibility to overwrite processing logic by providing
 * custom {@link Processor}.
 * 
 * @author Alexey Stashok
 */
public class StandaloneProcessorSelector implements ProcessorSelector {
    public static final StandaloneProcessorSelector INSTANCE =
            new StandaloneProcessorSelector();

    /**
     * Always return null, which means no {@link Processor} was found to process
     * {@link IOEvent}.
     */
    @Override
    public Processor select(IOEvent ioEvent, Connection connection) {
        return null;
    }

}
