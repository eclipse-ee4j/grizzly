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

package org.glassfish.grizzly.http2;

import java.util.LinkedList;

/**
 * The queue of element bundles. Each bundle in the queue may be empty or have some elements.
 */
final class BundleQueue<E> {
    private final LinkedList<Record<E>> internalQueue = new LinkedList<>();
    private int lastElementAbsoluteDistance;

    /**
     * Add the element to the specified bundle. The bundle is represented by its order in the queue. It is not possible to
     * add the element to a bundle, which already exists and is not the last bundle in the queue.
     *
     * @param bundle the bundle to which the specified <code>element</code> will be added.
     * @param element the element to add.
     */
    public void add(final int bundle, final E element) {
        if (lastElementAbsoluteDistance > bundle) {
            throw new IllegalStateException("New element must have greater" + " absolute distance than the last element in the queue");
        }
        internalQueue.addLast(new Record<>(element, bundle - lastElementAbsoluteDistance));
        lastElementAbsoluteDistance = bundle;
    }

    /**
     * Returns <tt>true</tt> if there is available element in the current bundle.
     */
    public boolean hasNext() {
        return !internalQueue.isEmpty() && internalQueue.getFirst().distance == 0;
    }

    /**
     * Returns next available element in the current bundle.
     */
    public E next() {
        if (!hasNext()) {
            throw new IllegalStateException("There is no next element available");
        }
        return internalQueue.removeFirst().value;
    }

    /**
     * Switches to the next bundle in the queue, all the unread elements from the previously active bundle will be removed.
     * 
     * @return <tt>true</tt> if next bundle exists and is not empty
     */
    public boolean nextBundle() {
        if (internalQueue.isEmpty()) {
            return false;
        }
        // skip old records
        while (internalQueue.getFirst().distance == 0) {
            internalQueue.removeFirst();
            if (internalQueue.isEmpty()) {
                return false;
            }
        }
        lastElementAbsoluteDistance--;
        return --internalQueue.getFirst().distance == 0;
    }

    private static final class Record<E> {

        private final E value;
        private int distance;

        public Record(final E value, final int distance) {
            this.value = value;
            this.distance = distance;
        }
    }

}
