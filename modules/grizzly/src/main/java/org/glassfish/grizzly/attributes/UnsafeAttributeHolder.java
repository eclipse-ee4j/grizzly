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

package org.glassfish.grizzly.attributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.function.Supplier;

/**
 * A non thread-safe {@link AttributeHolder} implementation.
 *
 * @author Alexey Stashok
 */
final class UnsafeAttributeHolder implements AttributeHolder {
    // the associated AttributeBuilder
    final DefaultAttributeBuilder attributeBuilder;
    // the associated IndexedAttributeAccessorImpl
    final IndexedAttributeAccessorImpl indexedAttributeAccessor;

    // list of "optimistic" holders to be used before going to a slower Map storage
    private final Holder h1 = new Holder();
    private final Holder h2 = new Holder();
    private final Holder h3 = new Holder();
    private final Holder h4 = new Holder();

    // the Map storage
    private Map<Integer, Object> valueMap;

    // true, if at least one element has been set
    private boolean isSet;

    UnsafeAttributeHolder(final DefaultAttributeBuilder attributeBuilder) {
        this.attributeBuilder = attributeBuilder;
        indexedAttributeAccessor = new IndexedAttributeAccessorImpl();
    }

    @Override
    public Object getAttribute(final String name) {
        return getAttribute(name, null);
    }

    @Override
    public Object getAttribute(final String name, final Supplier initializer) {

        if (!isSet && initializer == null) {
            return null;
        }

        final Attribute attribute = attributeBuilder.getAttributeByName(name);
        if (attribute != null) {
            return indexedAttributeAccessor.getAttribute(attribute, initializer);
        }

        return initializer != null ? initializer.get() : null;
    }

    @Override
    public void setAttribute(final String name, final Object value) {
        Attribute attribute = attributeBuilder.getAttributeByName(name);
        if (attribute == null) {
            attribute = attributeBuilder.createAttribute(name);
        }

        indexedAttributeAccessor.setAttribute(attribute, value);
    }

    @Override
    public Object removeAttribute(final String name) {
        if (!isSet) {
            return null;
        }

        final Attribute attribute = attributeBuilder.getAttributeByName(name);
        if (attribute != null) {
            return indexedAttributeAccessor.removeAttribute(attribute);
        }

        return null;
    }

    @Override
    public Set<String> getAttributeNames() {
        if (!isSet) {
            return null;
        }

        final Set<String> tmpSet = new HashSet<>(4);

        if (h1.isSet && h1.value != null) {
            tmpSet.add(attributeBuilder.getAttributeByIndex(h1.idx).name());
        }

        if (h2.isSet && h2.value != null) {
            tmpSet.add(attributeBuilder.getAttributeByIndex(h2.idx).name());
        }

        if (h3.isSet && h3.value != null) {
            tmpSet.add(attributeBuilder.getAttributeByIndex(h3.idx).name());
        }

        if (h4.isSet && h4.value != null) {
            tmpSet.add(attributeBuilder.getAttributeByIndex(h4.idx).name());
        }

        if (valueMap != null) {
            for (Integer idx : valueMap.keySet()) {
                tmpSet.add(attributeBuilder.getAttributeByIndex(idx).name());
            }
        }

        return tmpSet;
    }

    @Override
    public void clear() {
        if (!isSet) {
            return;
        }

        isSet = false;

        h1.clear();
        h2.clear();
        h3.clear();
        h4.clear();

        valueMap = null;
    }

    @Override
    public void recycle() {
        clear();
    }

    @Override
    public AttributeBuilder getAttributeBuilder() {
        return attributeBuilder;
    }

    @Override
    public IndexedAttributeAccessor getIndexedAttributeAccessor() {
        return indexedAttributeAccessor;
    }

    @Override
    public void copyFrom(final AttributeHolder srcAttributes) {
        if (srcAttributes == null) {
            throw new NullPointerException("srcAttributes can't be null");
        }

        if (srcAttributes instanceof UnsafeAttributeHolder) {
            // optimistic case
            final UnsafeAttributeHolder srcUnsafe = (UnsafeAttributeHolder) srcAttributes;
            if (!srcUnsafe.isSet) {
                clear();
                return;
            }

            isSet = true;
            h1.copyFrom(srcUnsafe.h1);
            h2.copyFrom(srcUnsafe.h2);
            h3.copyFrom(srcUnsafe.h3);
            h4.copyFrom(srcUnsafe.h4);

            if (valueMap != null || srcUnsafe.valueMap != null) {
                MapperAccessor.copy(srcUnsafe, this);
            }
        } else {
            // pessimistic case (slow)
            clear();

            final Set<String> names = srcAttributes.getAttributeNames();
            for (String name : names) {
                setAttribute(name, srcAttributes.getAttribute(name));
            }
        }
    }

    @Override
    public void copyTo(final AttributeHolder dstAttributes) {
        if (dstAttributes == null) {
            throw new NullPointerException("dstAttributes can't be null");
        }

        if (!isSet) {
            dstAttributes.clear();
            return;
        }

        if (dstAttributes instanceof UnsafeAttributeHolder) {
            // optimistic case
            final UnsafeAttributeHolder dstUnsafe = (UnsafeAttributeHolder) dstAttributes;

            dstUnsafe.isSet = true;
            dstUnsafe.h1.copyFrom(h1);
            dstUnsafe.h2.copyFrom(h2);
            dstUnsafe.h3.copyFrom(h3);
            dstUnsafe.h4.copyFrom(h4);

            if (valueMap != null || dstUnsafe.valueMap != null) {
                MapperAccessor.copy(this, dstUnsafe);
            }
        } else {
            // pessimistic case (slow)
            dstAttributes.clear();

            final Set<String> names = getAttributeNames();
            for (String name : names) {
                dstAttributes.setAttribute(name, getAttribute(name));
            }
        }
    }

    /**
     * {@link IndexedAttributeAccessor} implementation.
     */
    protected final class IndexedAttributeAccessorImpl implements IndexedAttributeAccessor {
        @Override
        public Object getAttribute(final int index) {
            return getAttribute(index, null);
        }

        @Override
        public Object getAttribute(final int index, final Supplier initializer) {
            if (!isSet && initializer == null) {
                return null;
            }

            return getAttribute(attributeBuilder.getAttributeByIndex(index), initializer);
        }

        @Override
        public void setAttribute(final int index, final Object value) {
            setAttribute(attributeBuilder.getAttributeByIndex(index), value);
        }

        @Override
        public Object removeAttribute(final int index) {
            return removeAttribute(attributeBuilder.getAttributeByIndex(index));
        }

        private Object getAttribute(final Attribute attribute, final Supplier initializer) {
            final int idx = attribute.index();

            final Holder h = holderByIdx(idx);

            if (h != null) {
                if (h.value == null && initializer != null) {
                    h.value = initializer.get();
                }

                return h.value;
            }

            Object value = valueMap != null ? MapperAccessor.getValue(UnsafeAttributeHolder.this, idx) : null;

            if (value == null && initializer != null) {
                value = initializer.get();
                setAttribute(attribute, value);
            }

            return value;
        }

        private Object setAttribute(final Attribute attribute, final Object value) {
            if (!isSet) {
                if (value != null) {
                    // optimized first value set
                    isSet = true;
                    h1.set(attribute.index(), value);
                }

                return null;
            }

            isSet = true;

            final int idx = attribute.index();

            Holder h = holderByIdx(idx);
            if (h != null) {
                return h.set(idx, value);
            }

            if (valueMap != null &&
            // we could use valueMap.contains(idx), but weak comparison is even better.
            // strong equals comparison will be executed inside MapperAccessor.setValue
                    valueMap.get(idx) != value) {

                // we go here only if we're sure the element is already in the map
                // and we need to update it
                return MapperAccessor.setValue(UnsafeAttributeHolder.this, idx, value);
            }

            // if the value is null - it means we supposed to remove the attribute.
            // but it wasn't found - so we can return
            if (value == null) {
                return null;
            }

            // Now we know there is no old value associated with the attribute
            // so we can find an empty holder to store the value
            h = emptyHolder();

            if (h != null) {
                h.set(idx, value);
                return null;
            }

            // there's no empty holder

            // check if there's a holder caching null value

            h = nullHolder();
            if (h != null) {
                // if yes - override it
                h.set(idx, value);
                return null;
            }

            // and finally if there's no other way around - just store the
            // value in the map
            return MapperAccessor.setValue(UnsafeAttributeHolder.this, idx, value);
        }

        private Object removeAttribute(final Attribute attribute) {
            return setAttribute(attribute, null);
        }

        private Holder holderByIdx(final int idx) {
            if (h1.is(idx)) {
                return h1;
            }

            if (h2.is(idx)) {
                return h2;
            }

            if (h3.is(idx)) {
                return h3;
            }

            if (h4.is(idx)) {
                return h4;
            }

            return null;
        }

        private Holder emptyHolder() {
            if (!h1.isSet) {
                return h1;
            }

            if (!h2.isSet) {
                return h2;
            }

            if (!h3.isSet) {
                return h3;
            }

            if (!h4.isSet) {
                return h4;
            }

            return null;
        }

        // we call this method, when we're sure all the holders are set
        private Holder nullHolder() {
            if (h1.value == null) {
                return h1;
            }

            if (h2.value == null) {
                return h2;
            }

            if (h3.value == null) {
                return h3;
            }

            if (h4.value == null) {
                return h4;
            }

            return null;
        }
    }

    private static final class Holder {
        int idx;
        Object value;

        boolean isSet;

        Object set(final int idx, final Object value) {
            final Object oldValue = this.value;
            this.idx = idx;
            this.value = value;
            isSet = true;

            return oldValue;
        }

        void clear() {
            if (isSet) {
                idx = -1;
                value = null;
                isSet = false;
            }
        }

        private boolean is(final int idx) {
            return isSet && this.idx == idx;
        }

        private void copyFrom(final Holder src) {
            isSet = src.isSet;
            idx = src.idx;
            value = src.value;
        }
    }

    private static final class MapperAccessor {
        private static Object getValue(final UnsafeAttributeHolder holder, final Integer idx) {
            return holder.valueMap.get(idx);
        }

        private static Object setValue(final UnsafeAttributeHolder holder, final Integer idx, final Object value) {
            if (value == null) {
                if (holder.valueMap != null) {
                    return holder.valueMap.remove(idx);
                }

                return null;
            }

            if (holder.valueMap == null) {
                holder.valueMap = new HashMap<>(4);
            }

            return holder.valueMap.put(idx, value);
        }

        private static void copy(final UnsafeAttributeHolder src, final UnsafeAttributeHolder dst) {
            if (src.valueMap != null) {
                if (dst.valueMap == null) {
                    dst.valueMap = new HashMap<>(src.valueMap.size());
                } else {
                    dst.valueMap.clear();
                }

                dst.valueMap.putAll(src.valueMap);
            } else {
                dst.valueMap = null;
            }
        }
    }
}
