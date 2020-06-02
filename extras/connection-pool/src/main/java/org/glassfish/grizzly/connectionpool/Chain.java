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

import java.util.LinkedList;

/**
 * Minimalistic linked list implementation. This implementation doesn't work directly with objects, but their
 * {@link Link}s, so there is no performance penalty for locating object in the list.
 *
 * The <tt>Chain</tt> implementation is not thread safe.
 *
 * @author Alexey Stashok
 */
final class Chain<E> {
    /**
     * The size of the chain (number of elements stored).
     */
    private int size;

    /**
     * The first link in the chain
     */
    private Link<E> firstLink;
    /**
     * The last link in the chain
     */
    private Link<E> lastLink;

    /**
     * Returns <tt>true</tt> if this <tt>Chain</tt> doesn't have any element stored, or <tt>false</tt> otherwise.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements stored in this <tt>Chain<tt>.
     */
    public int size() {
        return size;
    }

    /**
     * Returns the first {@link Link} in this <tt>Chain</tt>.
     */
    public Link<E> getFirstLink() {
        return firstLink;
    }

    /**
     * Returns the last {@link Link} in this <tt>Chain</tt>.
     */
    public Link<E> getLastLink() {
        return lastLink;
    }

    /**
     * Adds a {@link Link} to the beginning of this <tt>Chain</tt>.
     */
    public void offerFirst(final Link<E> link) {
        if (link.isAttached()) {
            throw new IllegalStateException("Already linked");
        }

        link.next = firstLink;
        if (firstLink != null) {
            firstLink.prev = link;
        }

        firstLink = link;
        if (lastLink == null) {
            lastLink = firstLink;
        }

        link.attach();

        size++;
    }

    /**
     * Adds a {@link Link} to the end of this <tt>Chain</tt>.
     */
    public void offerLast(final Link<E> link) {
        if (link.isAttached()) {
            throw new IllegalStateException("Already linked");
        }

        link.prev = lastLink;
        if (lastLink != null) {
            lastLink.next = link;
        }

        lastLink = link;
        if (firstLink == null) {
            firstLink = lastLink;
        }

        link.attach();

        size++;
    }

    /**
     * Removes and returns the last {@link Link} of this <tt>Chain<tt>.
     */
    public Link<E> pollLast() {
        if (lastLink == null) {
            return null;
        }

        final Link<E> link = lastLink;
        lastLink = link.prev;
        if (lastLink == null) {
            firstLink = null;
        } else {
            lastLink.next = null;
        }

        link.detach();

        size--;

        return link;
    }

    /**
     * Removes and returns the first {@link Link} of this <tt>Chain<tt>.
     */
    public Link<E> pollFirst() {
        if (firstLink == null) {
            return null;
        }

        final Link<E> link = firstLink;
        firstLink = link.next;
        if (firstLink == null) {
            lastLink = null;
        } else {
            firstLink.prev = null;
        }

        link.detach();

        size--;

        return link;
    }

    /**
     * Removes the {@link Link} from this <tt>Chain<tt>. Unlike {@link LinkedList#remove(java.lang.Object)}, this operation
     * is cheap, because the {@link Link} already has information about its location in the <tt>Chain<tt>, so no additional
     * lookup needed.
     *
     * @param link the {@link Link} to be removed.
     */
    public boolean remove(final Link<E> link) {
        if (!link.isAttached()) {
            return false;
        }

        final Link<E> prev = link.prev;
        final Link<E> next = link.next;
        if (prev != null) {
            prev.next = next;
        }

        if (next != null) {
            next.prev = prev;
        }

        link.detach();

        if (lastLink == link) {
            lastLink = prev;
            if (lastLink == null) {
                firstLink = null;
            }
        } else if (firstLink == link) {
            firstLink = next;
        }

        size--;

        return true;
    }

    /**
     * Moves the {@link Link} towards the <tt>Chain</tt>'s head by 1 element. If the {@link Link} is already located at the
     * <tt>Chain</tt>'s head - the method invocation will not have any effect.
     *
     * @param link the {@link Link} to be moved.
     */
    public void moveTowardsHead(final Link<E> link) {
        final Link<E> prev = link.prev;

        // check if this is head
        if (prev == null) {
            return;
        }

        final Link<E> next = link.next;
        final Link<E> prevPrev = prev.prev;

        if (prevPrev != null) {
            prevPrev.next = link;
        }

        link.prev = prevPrev;
        link.next = prev;

        prev.prev = link;
        prev.next = next;

        if (next != null) {
            next.prev = prev;
        }
    }
}
