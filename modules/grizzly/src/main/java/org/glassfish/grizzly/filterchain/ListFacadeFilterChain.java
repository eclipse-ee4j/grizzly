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

package org.glassfish.grizzly.filterchain;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * {@link FilterChain} facade, which implements all the {@link List} related
 * methods.
 *
 * @see FilterChain
 * 
 * @author Alexey Stashok
 */
public abstract class ListFacadeFilterChain extends AbstractFilterChain {
    
    /**
     * The list of Filters this chain will invoke.
     */
    protected final List<Filter> filters;

    public ListFacadeFilterChain(final List<Filter> filtersImpl) {
        this.filters = filtersImpl;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(Filter filter) {
        if (filters.add(filter)) {
            filter.onAdded(this);
            notifyChangedExcept(filter);
            return true;
        }

        return false;
    }
        
    /**
     * {@inheritDoc}
     */
    @Override
    public void add(int index, Filter filter){
        filters.add(index, filter);
        filter.onAdded(this);
        notifyChangedExcept(filter);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(Collection<? extends Filter> c) {
        for(Filter filter : c) {
            filters.add(filter);
            filter.onAdded(this);
        }

        notifyChangedExcept(null);
        
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean addAll(int index, Collection<? extends Filter> c) {
        int i = 0;
        for(Filter filter : c) {
            filters.add(index + (i++), filter);
            filter.onAdded(this);
        }

        notifyChangedExcept(null);

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter set(final int index, final Filter filter) {
        final Filter oldFilter = filters.set(index, filter);
        if (oldFilter != filter) {
            if (oldFilter != null) {
                oldFilter.onRemoved(this);
            }
            
            filter.onAdded(this);
            notifyChangedExcept(filter);
        }

        return oldFilter;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final Filter get(final int index) {
        return filters.get(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(final Object object) {
        return filters.indexOf(object);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(Object filter) {
        return filters.lastIndexOf(filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings({""})
    public boolean contains(Object filter) {
        return filters.contains(filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return filters.containsAll(c);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] toArray() {
        return filters.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return filters.toArray(a);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(Object object) {
        final Filter filter = (Filter) object;

        if (filters.remove(filter)) {
            filter.onRemoved(this);
            notifyChangedExcept(filter);
            return true;
        }

        return false;
    }
           
    /**
     * {@inheritDoc}
     */
    @Override
    public Filter remove(int index) {
        final Filter filter = filters.remove(index);
        if (filter != null) {
            filter.onRemoved(this);
            notifyChangedExcept(filter);
            return filter;
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return filters == null || filters.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size() {
        return filters.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        final Object[] localFilters = filters.toArray();
        filters.clear();
        
        for (Object filter : localFilters) {
            ((Filter) filter).onRemoved(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Filter> iterator() {
        return filters.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<Filter> listIterator() {
        return filters.listIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ListIterator<Filter> listIterator(int index) {
        return filters.listIterator(index);
    }

    protected void notifyChangedExcept(Filter filter) {
        for(Filter currentFilter : filters) {
            if (currentFilter != filter) {
                currentFilter.onFilterChainChanged(this);
            }
        }
    }
}
