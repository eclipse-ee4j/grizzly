/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.osgi.httpservice;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * Grizzly OSGi {@link HttpService} {@link ServiceFactory}.
 *
 * @author Hubert Iwaniuk
 * @since Jan 20, 2009
 */
public class HttpServiceFactory implements ServiceFactory {
    private final Logger logger;
    private final OSGiMainHandler mainHttpHandler;

    public HttpServiceFactory(HttpServer httpServer, Logger logger, Bundle bundle) {
        this.logger = logger;
        mainHttpHandler = new OSGiMainHandler(logger, bundle);
        httpServer.getServerConfiguration().addHttpHandler(mainHttpHandler, "/");
    }

    @Override
    public HttpService getService(
            Bundle bundle, ServiceRegistration serviceRegistration) {
        logger.info("Bundle: " + bundle + ", is getting HttpService with serviceRegistration: " + serviceRegistration);
        return new HttpServiceImpl(bundle, logger);
    }

    @Override
    public void ungetService(
            Bundle bundle, ServiceRegistration serviceRegistration,
            Object httpServiceObj) {
        logger.info("Bundle: " + bundle + ", is ungetting HttpService with serviceRegistration: " + serviceRegistration);
        mainHttpHandler.uregisterAllLocal();
    }

    /**
     * Clean up.
     */
    public void stop() {
        logger.info("Stoping main handler");
        mainHttpHandler.unregisterAll();
    }
}
