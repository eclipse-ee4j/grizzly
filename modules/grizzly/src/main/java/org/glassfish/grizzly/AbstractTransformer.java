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

import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeBuilder;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.memory.MemoryManager;

/**
 *
 * @author Alexey Stashok
 */
public abstract class AbstractTransformer<K, L> implements Transformer<K, L> {
    protected final AttributeBuilder attributeBuilder = Grizzly.DEFAULT_ATTRIBUTE_BUILDER;

    protected final Attribute<LastResultAwareState<K, L>> stateAttr;

    private MemoryManager memoryManager;

    public AbstractTransformer() {
        String namePrefix = getNamePrefix();

        stateAttr = attributeBuilder.createAttribute(namePrefix + ".state");
    }

    protected String getNamePrefix() {
        return getClass().getName();
    }

    @Override
    public final TransformationResult<K, L> transform(AttributeStorage storage, K input) throws TransformationException {
        return saveLastResult(storage, transformImpl(storage, input));
    }

    protected abstract TransformationResult<K, L> transformImpl(AttributeStorage storage, K input) throws TransformationException;

    @Override
    public final TransformationResult<K, L> getLastResult(final AttributeStorage storage) {
        final LastResultAwareState<K, L> state = stateAttr.get(storage);
        if (state != null) {
            return state.getLastResult();
        }

        return null;
    }

    protected final TransformationResult<K, L> saveLastResult(final AttributeStorage storage, final TransformationResult<K, L> result) {
        obtainStateObject(storage).setLastResult(result);
        return result;
    }

    @Override
    public void release(AttributeStorage storage) {
        stateAttr.remove(storage);
    }

    protected MemoryManager obtainMemoryManager(AttributeStorage storage) {
        if (memoryManager != null) {
            return memoryManager;
        }

        if (storage instanceof Connection) {
            Connection connection = (Connection) storage;
            return connection.getMemoryManager();
        }

        return MemoryManager.DEFAULT_MEMORY_MANAGER;
    }

    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
    }

    public static <T> T getValue(final AttributeStorage storage, final Attribute<T> attribute, final T defaultValue) {
        final T value = attribute.get(storage);
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    protected final LastResultAwareState<K, L> obtainStateObject(final AttributeStorage storage) {

        LastResultAwareState<K, L> value = stateAttr.get(storage);
        if (value == null) {
            value = createStateObject();
            stateAttr.set(storage, value);
        }

        return value;
    }

    protected LastResultAwareState<K, L> createStateObject() {
        return new LastResultAwareState<>();
    }

    public static class LastResultAwareState<K, L> {
        protected TransformationResult<K, L> lastResult;

        public TransformationResult<K, L> getLastResult() {
            return lastResult;
        }

        public void setLastResult(TransformationResult<K, L> lastResult) {
            this.lastResult = lastResult;
        }
    }
}
