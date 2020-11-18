/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.connectionpool;

/**
 * The object represents an element, from which a minimalistic {@link Chain} is built.
 *
 * It is possible to attach or detach a <tt>Link</tt> from a {@link Chain}. When the <tt>Link</tt> is attached it
 * contains a pointers to the previous and the next <tt>Link</tt>s in the {@link Chain}, otherwise, if the <tt>Link</tt>
 * is detached - the pointers values are <tt>null</tt>.
 *
 * If a <tt>Link</tt> is attached - it can only be attached to one {@link Chain}.
 *
 * @param <E>
 * @author Alexey Stashok
 */
public final class Link<E> {
    /**
     * The Link payload/value
     */
    private final E value;

    /**
     * The pointer to the previous link in the chain
     */
    Link<E> prev;
    /**
     * The pointer to the next link in the chain
     */
    Link<E> next;

    /**
     * attachment flag
     */
    private boolean isAttached;
    /**
     * The attachment timestamp, which shows the time when the link was attached.
     */
    private long linkTimeStamp = -1;

    /**
     * Construct the <tt>Link</tt> holding given value object.
     * 
     * @param value an object the <tt>Link</tt> represents.
     */
    public Link(final E value) {
        this.value = value;
    }

    /**
     * @return the value held by this {@link Link}.
     */
    public E getValue() {
        return value;
    }

    /**
     * Attaches the <tt>Link</tt> to a {@link Chain}.
     */
    void attach() {
        linkTimeStamp = System.currentTimeMillis();
        isAttached = true;
    }

    /**
     * Detaches the <tt>Link</tt> from a {@link Chain}.
     */
    void detach() {
        isAttached = false;
        linkTimeStamp = -1;
        prev = next = null;
    }

    /**
     * @return the timestamp, that represents the time (in milliseconds) when the <tt>Link</tt> was attached to a
     * {@link Chain}, or <tt>-1</tt> if the <tt>Link</tt> is not currently attached to a {@link Chain}.
     */
    public long getAttachmentTimeStamp() {
        return linkTimeStamp;
    }

    /**
     * @return <tt>true</tt> if the <tt>Link</tt> is currently attached to a {@link Chain} or <tt>false</tt> otherwise.
     */
    public boolean isAttached() {
        return isAttached;
    }

    @Override
    public String toString() {
        return "Link{" + "value=" + value + ", prev=" + prev + ", next=" + next + ", isAttached=" + isAttached + ", linkTimeStamp=" + linkTimeStamp + "} "
                + super.toString();
    }
}
