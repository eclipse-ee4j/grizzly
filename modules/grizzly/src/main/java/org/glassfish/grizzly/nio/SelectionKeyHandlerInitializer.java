/*
 * Copyright (c) 2012, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Grizzly;

import java.util.logging.Level;
import java.util.logging.Logger;

class SelectionKeyHandlerInitializer {
    
    private static final String PROP = "org.glassfish.grizzly.DEFAULT_SELECTION_KEY_HANDLER";
    
    private static final Logger LOGGER = Grizzly.logger(SelectionKeyHandlerInitializer.class);

    @SuppressWarnings("unchecked")
    static SelectionKeyHandler initHandler() {
        final String className = System.getProperty(PROP);
        if (className != null) {
            try {
                Class<? extends SelectionKeyHandler> handlerClass = (Class<? extends SelectionKeyHandler>)
                        Class.forName(className, 
                                      true, 
                                      SelectionKeyHandler.class.getClassLoader());
                return handlerClass.newInstance();
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.SEVERE)) {
                    LOGGER.log(Level.SEVERE,
                            "Unable to load or create a new instance of SelectionKeyHandler {0}.  Cause: {1}",
                            new Object[]{className, e.getMessage()});
                }
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, e.toString(), e);
                }
                return new DefaultSelectionKeyHandler();
            }
        }
        return new DefaultSelectionKeyHandler();
    }

}
