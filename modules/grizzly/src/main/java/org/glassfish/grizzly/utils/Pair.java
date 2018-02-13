/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
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
 * Key : Value pair implementation.
 *
 * @author Alexey Stashok
 */
public class Pair<K, L> implements PoolableObject {
    private K first;
    private L second;

    public Pair() {
    }

    public Pair(K first, L second) {
        this.first = first;
        this.second = second;
    }

    public K getFirst() {
        return first;
    }

    public void setFirst(K first) {
        this.first = first;
    }

    public L getSecond() {
        return second;
    }

    public void setSecond(L second) {
        this.second = second;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void release() {
        first = null;
        second = null;
    }

    @Override
    public String toString() {
        return "Pair{" + "key=" + first + " value=" + second + '}';
    }
}
