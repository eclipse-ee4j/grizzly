/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.memory;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 *
 * @author oleksiys
 */
public abstract class AbstractBufferArray<E> {
    protected final Class<E> clazz;
    private E[] byteBufferArray;
    private PosLim[] initStateArray;

    private int size;

    protected abstract void setPositionLimit(E buffer, int position, int limit);

    protected abstract int getPosition(E buffer);

    protected abstract int getLimit(E buffer);

    @SuppressWarnings("unchecked")
    protected AbstractBufferArray(Class<E> clazz) {
        this.clazz = clazz;
        byteBufferArray = (E[]) Array.newInstance(clazz, 4);
        initStateArray = new PosLim[4];
    }

    public void add(final E byteBuffer) {
        add(byteBuffer, getPosition(byteBuffer), getLimit(byteBuffer));
    }

    public void add(final E byteBuffer, final int restorePosition, final int restoreLimit) {

        ensureCapacity(1);
        byteBufferArray[size] = byteBuffer;
        PosLim poslim = initStateArray[size];
        if (poslim == null) {
            poslim = new PosLim();
            initStateArray[size] = poslim;
        }

        poslim.initialPosition = getPosition(byteBuffer);
        poslim.initialLimit = getLimit(byteBuffer);
        poslim.restorePosition = restorePosition;
        poslim.restoreLimit = restoreLimit;

        size++;
    }

    public E[] getArray() {
        return byteBufferArray;
    }

    public void restore() {
        for (int i = 0; i < size; i++) {
            final PosLim poslim = initStateArray[i];
            setPositionLimit(byteBufferArray[i], poslim.restorePosition, poslim.restoreLimit);
        }
    }

    public final int getInitialPosition(final int idx) {
        return initStateArray[idx].initialPosition;
    }

    public int getInitialLimit(final int idx) {
        return initStateArray[idx].initialLimit;
    }

    public final int getInitialBufferSize(final int idx) {
        return getInitialLimit(idx) - getInitialPosition(idx);
    }

    public int size() {
        return size;
    }

    private void ensureCapacity(final int grow) {
        final int diff = byteBufferArray.length - size;
        if (diff >= grow) {
            return;
        }

        final int newSize = Math.max(diff + size, byteBufferArray.length * 3 / 2 + 1);
        byteBufferArray = Arrays.copyOf(byteBufferArray, newSize);
        initStateArray = Arrays.copyOf(initStateArray, newSize);
    }

    public void reset() {
        Arrays.fill(byteBufferArray, 0, size, null);
        size = 0;
    }

    public void recycle() {
        reset();
    }

    private final static class PosLim {
        int initialPosition;
        int initialLimit;

        int restorePosition;
        int restoreLimit;
    }
}
