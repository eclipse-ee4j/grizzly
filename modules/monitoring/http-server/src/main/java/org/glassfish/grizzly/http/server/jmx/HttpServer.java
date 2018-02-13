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

package org.glassfish.grizzly.http.server.jmx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * JMX management object for {@link org.glassfish.grizzly.http.server.HttpServer}.
 *
 * @since 2.0
 */
@ManagedObject
@Description("The HttpServer.")
public class HttpServer extends JmxObject {


    private final org.glassfish.grizzly.http.server.HttpServer gws;

    private GrizzlyJmxManager mom;
    private final ConcurrentMap<String, NetworkListener> currentListeners =
            new ConcurrentHashMap<>(4);
    private final ConcurrentMap<String, Object> listenersJmx =
            new ConcurrentHashMap<>(4);
    


    // ------------------------------------------------------------ Constructors


    public HttpServer(org.glassfish.grizzly.http.server.HttpServer gws) {
        this.gws = gws;
    }


    // -------------------------------------------------- Methods from JmxObject


    /**
     * {@inheritDoc}
     */
    @Override
    public String getJmxName() {
        return "HttpServer";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
        this.mom = mom;
        rebuildSubTree();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized void onDeregister(GrizzlyJmxManager mom) {
        this.mom = null;
    }


    // ---------------------------------------------------------- Public Methods


    /**
     * @see org.glassfish.grizzly.http.server.HttpServer#isStarted()
     */
    @ManagedAttribute(id="started")
    @Description("Indicates whether or not this server instance has been started.")
    public boolean isStarted() {
        return gws.isStarted();
    }


//    @ManagedAttribute(id="document-root")
//    @Description("The document root of this server instance.")
//    public Collection<String> getDocumentRoots() {
//        return gws.getServerConfiguration().getDocRoots();
//    }


    // ------------------------------------------------------- Protected Methods


    protected void rebuildSubTree() {

        for (final NetworkListener l : gws.getListeners()) {
            final NetworkListener currentListener = currentListeners.get(l.getName());
            if (currentListener != l) {
                if (currentListener != null) {
                    final Object listenerJmx = listenersJmx.get(l.getName());
                    if (listenerJmx != null) {
                        mom.deregister(listenerJmx);
                    }

                    currentListeners.remove(l.getName());
                    listenersJmx.remove(l.getName());
                }

                final Object mmJmx = l.createManagementObject();
                mom.register(this, mmJmx, "NetworkListener[" + l.getName() + ']');
                currentListeners.put(l.getName(), l);
                listenersJmx.put(l.getName(), mmJmx);
            }
        }
        
    }
}
