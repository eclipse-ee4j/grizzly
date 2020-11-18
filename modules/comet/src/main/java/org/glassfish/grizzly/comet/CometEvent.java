/*
 * Copyright (c) 2007, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.comet;

/**
 * Simple event class used to pass information between {@link CometHandler} and the Comet implementation.
 *
 * @author Jeanfrancois Arcand
 */
public class CometEvent<E> {
    public enum Type {
        INTERRUPT, NOTIFY, INITIALIZE, TERMINATE, READ, WRITE,
    }

    /**
     * This type of event.
     */
    protected Type type;
    /**
     * Share an <code>E</code> amongst {@link CometHandler}
     */
    protected E attachment;
    /**
     * The CometContext from where this instance was fired.
     */
    private CometContext cometContext;
    private static final long serialVersionUID = 920798330036889926L;

    /**
     * Create a new <code>CometEvent</code>
     */
    public CometEvent() {
        type = Type.NOTIFY;
    }

    public CometEvent(Type type) {
        this.type = type;
    }

    public CometEvent(Type type, CometContext context) {
        this.type = type;
        cometContext = context;
    }

    public CometEvent(Type type, CometContext cometContext, E attachment) {
        this.type = type;
        this.attachment = attachment;
        this.cometContext = cometContext;
    }

    /**
     * Return the <code>type</code> of this object.
     *
     * @return int Return the <code>type</code> of this object
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the <code>type</code> of this object.
     *
     * @param type the <code>type</code> of this object
     */
    protected void setType(Type type) {
        this.type = type;
    }

    /**
     * Attach an <E>
     *
     * @param attachment An attachment.
     */
    public void attach(E attachment) {
        this.attachment = attachment;
    }

    /**
     * Return the attachment <E>
     *
     * @return attachment An attachment.
     */
    public E attachment() {
        return attachment;
    }

    /**
     * Return the {@link CometContext} that fired this event.
     */
    public CometContext getCometContext() {
        return cometContext;
    }

    /**
     * Set the {@link CometContext} that fired this event.
     */
    protected void setCometContext(CometContext cometContext) {
        this.cometContext = cometContext;
    }
}
