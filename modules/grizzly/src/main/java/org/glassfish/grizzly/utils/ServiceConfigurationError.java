/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.utils;

/**
 * Error thrown when something goes wrong while looking up service providers. In particular, this error will be thrown
 * in the following situations:
 *
 * <ul>
 * <li>A concrete provider class cannot be found,
 * <li>A concrete provider class cannot be instantiated,
 * <li>The format of a provider-configuration file is illegal, or
 * <li>An IOException occurs while reading a provider-configuration file.
 * </ul>
 *
 * @author Mark Reinhold
 * @version 1.7, 03/12/19
 * @since 1.3
 */
public class ServiceConfigurationError extends Error {

    /**
     * Constructs a new instance with the specified detail string.
     */
    public ServiceConfigurationError(String message) {
        super(message);
    }

    /**
     * Constructs a new instance that wraps the specified throwable.
     */
    public ServiceConfigurationError(Throwable throwable) {
        super(throwable);
    }

}
