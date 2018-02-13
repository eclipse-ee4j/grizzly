/*
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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


import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * N-ary tree node implementation to support HTTP/2 stream hierarchies.
 */
public abstract class Node {

    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    protected static final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    protected static final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    protected final int id;
    protected Node next;
    protected Node prev;
    protected Node parent;
    protected Node firstChild;
    protected boolean exclusive;


    // ----------------------------------------------------------- Constructors


    protected Node(final int id) {
        this.id = id;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Mark this {@link Node} as exclusive.  Any siblings will be migrated
     * to the children list.
     */
    protected void exclusive() {
        writeLock.lock();
        try {
            final Node p = parent;
            p.detach(id);
            p.addChild(this, true);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add a sibling to this {@link Node}.
     */
    protected void addSibling(final Node sibling) {
        writeLock.lock();
        try {
            sibling.next = this;
            this.prev = sibling;
            sibling.parent = this.parent;
            parent.firstChild = sibling;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add a child to this {@link Node}.
     */
    protected void addChild(final Node n) {
        addChild(n, false);
    }

    /**
     * Add a new child. If the child is marked as exclusive, any
     * other children will be moved to become the children of this
     * exclusive child.
     */
    protected void addChild(final Node nodeBeingAddedAsChild, final boolean exclusive) {
        writeLock.lock();
        try {
            if (exclusive) {
                nodeBeingAddedAsChild.exclusive = true;
                if (nodeBeingAddedAsChild.firstChild != null && firstChild != null) {
                    Node tail = firstChild;
                    while (tail.next != null) {
                        tail = tail.next;
                    }
                    tail.next = nodeBeingAddedAsChild.firstChild;
                    nodeBeingAddedAsChild.firstChild.prev = tail;
                    nodeBeingAddedAsChild.firstChild = firstChild;
                } else if (nodeBeingAddedAsChild.firstChild == null && firstChild != null) {
                    nodeBeingAddedAsChild.firstChild = firstChild;
                }
                firstChild = null;
                if (nodeBeingAddedAsChild.firstChild != null) {
                    Node t = nodeBeingAddedAsChild.firstChild;
                    do {
                        t.parent = nodeBeingAddedAsChild;
                    } while ((t = t.next) != null);
                }
            }
            if (firstChild == null) {
                firstChild = nodeBeingAddedAsChild;
                firstChild.parent = this;
            } else {
                firstChild.addSibling(nodeBeingAddedAsChild);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Detail this {@link Node} from the tree maintaining any children.
     */
    protected Node detach(final int id) {
        return remove(id, true);
    }

    /**
     * Remove this {@link Node} from the tree.  Any children will be moved up
     * as a child of the remove {@link Node}'s parent.
     */
    protected Node remove(final int id) {
        return remove(id, false);
    }

    /**
     * Top down search from this {@link Node} and any children (recursively)
     * returning the node with a matching <code>id</code>.
     */
    protected Node find(final int id) {
        if (this.id == id) {
            return this;
        }
        readLock.lock();
        try {
            if (firstChild != null) {
                Node n = firstChild;
                do {
                    if (n.id == id) {
                        return n;
                    }
                    Node result = n.find(id);
                    if (result != null) {
                        return result;
                    }
                } while ((n = n.next) != null);
            }
            return null;
        } finally {
            readLock.unlock();
        }
    }


    // -------------------------------------------------------- Private Methods


    private boolean isFirstSibling() {
        return (next != null && prev == null);
    }

    private boolean isLastSibling() {
        return (next == null && prev != null);
    }

    private boolean hasSiblings() {
        return (next != null || prev != null);
    }

    private Node remove(final int id, final boolean retainChildren) {
        final Node n = find(id);
        if (n != null) {
            writeLock.lock();
            try {
                // remove this node from sibling pointer chains
                if (n.hasSiblings()) {
                    final Node left = n.prev;
                    final Node right = n.next;
                    if (n.isFirstSibling()) {
                        right.parent.firstChild = right;
                        right.prev = null;
                    } else if (n.isLastSibling()) {
                        left.next = null;
                    } else {
                        // Middle child!
                        left.next = right;
                        right.prev = left;
                    }
                }

                // re-parent the children to this node's parent and
                // push these children to the front of the child new parent child list
                if (!retainChildren) {
                    final Node np = n.parent;
                    if (n.firstChild != null) {
                        Node t = n.firstChild;
                        Node last = null;
                        do {
                            t.parent = np;
                            // quick look ahead to see if this node will be the last
                            if (t.next == null) {
                                last = t;
                            }
                        } while ((t = t.next) != null);

                        // 'push' the current child to the 'end' of children of the removed node
                        last.next = np.firstChild;
                        np.firstChild.prev = last;

                        // Set the new pointer to the new first child.
                        np.firstChild = n.firstChild;
                    }
                }

                // clear pointers and return
                n.parent = null;
                n.next = null;
                n.prev = null;
                if (!retainChildren) {
                    n.firstChild = null;
                }
                return n;
            } finally {
                writeLock.unlock();
            }
        }
        return null;
    }
}
