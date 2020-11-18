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

package org.glassfish.grizzly.filterchain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link FilterChainBuilder} implementation, which is responsible for constructing {@link FilterChain}s.
 *
 * @author Alexey Stashok
 */
public abstract class FilterChainBuilder {
    protected final List<Filter> patternFilterChain;

    private FilterChainBuilder() {
        patternFilterChain = new ArrayList<>();
    }

    public static FilterChainBuilder stateless() {
        return new StatelessFilterChainBuilder();
    }

    public static FilterChainBuilder stateful() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public abstract FilterChain build();

    public FilterChainBuilder add(Filter filter) {
        return addLast(filter);
    }

    public FilterChainBuilder addFirst(Filter filter) {
        patternFilterChain.add(0, filter);
        return this;
    }

    public FilterChainBuilder addLast(Filter filter) {
        patternFilterChain.add(filter);
        return this;
    }

    public FilterChainBuilder add(int index, Filter filter) {
        patternFilterChain.add(index, filter);
        return this;
    }

    public FilterChainBuilder set(int index, Filter filter) {
        patternFilterChain.set(index, filter);
        return this;
    }

    public Filter get(int index) {
        return patternFilterChain.get(index);
    }

    public FilterChainBuilder remove(int index) {
        patternFilterChain.remove(index);
        return this;
    }

    public FilterChainBuilder remove(Filter filter) {
        patternFilterChain.remove(filter);
        return this;
    }

    public FilterChainBuilder addAll(Filter[] array) {
        patternFilterChain.addAll(patternFilterChain.size(), Arrays.asList(array));
        return this;
    }

    public FilterChainBuilder addAll(int filterIndex, Filter[] array) {
        patternFilterChain.addAll(filterIndex, Arrays.asList(array));
        return this;
    }

    public FilterChainBuilder addAll(List<Filter> list) {
        return addAll(patternFilterChain.size(), list);
    }

    public FilterChainBuilder addAll(int filterIndex, List<Filter> list) {
        patternFilterChain.addAll(filterIndex, list);
        return this;
    }

    public FilterChainBuilder addAll(final FilterChainBuilder source) {
        patternFilterChain.addAll(source.patternFilterChain);
        return this;
    }

    public int indexOf(final Filter filter) {
        return patternFilterChain.indexOf(filter);
    }

    public int indexOfType(final Class<? extends Filter> filterType) {
        final int size = patternFilterChain.size();
        for (int i = 0; i < size; i++) {
            final Filter filter = get(i);
            if (filterType.isAssignableFrom(filter.getClass())) {
                return i;
            }
        }

        return -1;
    }

    public static class StatelessFilterChainBuilder extends FilterChainBuilder {
        @Override
        public FilterChain build() {
            final FilterChain fc = new DefaultFilterChain();
            fc.addAll(patternFilterChain);
            return fc;
        }
    }
}
