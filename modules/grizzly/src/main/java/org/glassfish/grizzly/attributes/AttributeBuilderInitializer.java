/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.attributes;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;

class AttributeBuilderInitializer {

    private static final String PROP = "org.glassfish.grizzly.DEFAULT_ATTRIBUTE_BUILDER";

    private static final Logger LOGGER = Grizzly.logger(AttributeBuilderInitializer.class);

    @SuppressWarnings("unchecked")
    static AttributeBuilder initBuilder() {
        final String className = System.getProperty(PROP);
        if (className != null) {
            try {
                Class<? extends AttributeBuilder> builderClass = (Class<? extends AttributeBuilder>) Class.forName(className, true,
                        AttributeBuilder.class.getClassLoader());
                return builderClass.newInstance();
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE, "Unable to load or create a new instance of AttributeBuilder {0}.  Cause: {1}",
                            new Object[] { className, e.getMessage() });
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, e.toString(), e);
                }
                return new DefaultAttributeBuilder();
            }
        }
        return new DefaultAttributeBuilder();
    }

}
