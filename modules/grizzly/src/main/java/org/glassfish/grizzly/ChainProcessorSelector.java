/*
 * Copyright (c) 2008, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link ProcessorSelector} implementation, which acts like wrapper for chain of {@link ProcessorSelector}s. So, when
 * {@link ProcessorSelector#select(IOEvent, Connection)} operation is called - it delegates selecting to the first
 * {@link ProcessorSelector} from chain. If first {@link ProcessorSelector} returns not <tt>null</tt> {@link Processor}
 * - {@link ChainProcessorSelector} returns it as result, otherwise next {@link ProcessorSelector} will be taken from
 * chain... etc
 *
 * @author Alexey Stashok
 */
public class ChainProcessorSelector implements ProcessorSelector, List<ProcessorSelector> {

    private final List<ProcessorSelector> selectorChain;

    public ChainProcessorSelector() {
        this(new ArrayList<ProcessorSelector>());
    }

    public ChainProcessorSelector(ProcessorSelector... selectorChain) {
        this(new ArrayList<>(Arrays.asList(selectorChain)));
    }

    public ChainProcessorSelector(List<ProcessorSelector> selectorChain) {
        this.selectorChain = selectorChain;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor select(IOEvent ioEvent, Connection connection) {
        for (ProcessorSelector processorSelector : selectorChain) {
            Processor processor = processorSelector.select(ioEvent, connection);
            if (processor != null) {
                return processor;
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return selectorChain.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return selectorChain.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(Object o) {
        return selectorChain.contains(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ProcessorSelector> iterator() {
        return selectorChain.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray() {
        return selectorChain.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return selectorChain.toArray(a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(ProcessorSelector o) {
        return selectorChain.add(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object o) {
        return selectorChain.remove(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return selectorChain.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends ProcessorSelector> c) {
        return selectorChain.addAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int index, Collection<? extends ProcessorSelector> c) {
        return selectorChain.addAll(index, c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        return selectorChain.removeAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return selectorChain.retainAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        selectorChain.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessorSelector get(int index) {
        return selectorChain.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessorSelector set(int index, ProcessorSelector element) {
        return selectorChain.set(index, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, ProcessorSelector element) {
        selectorChain.add(index, element);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessorSelector remove(int index) {
        return selectorChain.remove(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(Object o) {
        return selectorChain.indexOf(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(Object o) {
        return selectorChain.lastIndexOf(o);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<ProcessorSelector> listIterator() {
        return selectorChain.listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<ProcessorSelector> listIterator(int index) {
        return selectorChain.listIterator(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProcessorSelector> subList(int fromIndex, int toIndex) {
        return selectorChain.subList(fromIndex, toIndex);
    }
}
