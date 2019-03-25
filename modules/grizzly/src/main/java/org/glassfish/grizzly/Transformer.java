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

package org.glassfish.grizzly;

import org.glassfish.grizzly.attributes.AttributeStorage;

/**
 * <tt>Transformer</tt> interface, which knows how to transform the original
 * data to some custom representation.
 * A <tt>Transformer</tt> implementation could be stateful or stateless. However
 * it's very easy to write stateful <tt>Transformer</tt>, which actually doesn't
 * save any state internally, but uses {@link AttributeStorage} as an external
 * state storage. Please note, that {@link AttributeStorage} is being passed
 * as the parameter to all <tt>Transformer</tt> methods. This way it's
 * possible to reuse single instance of a stateful <tt>Transformer</tt> to
 * process lots of concurrent transformations.
 *
 * @author Alexey Stashok
 */
public interface Transformer<K, L> {
    /**
     * Get the <tt>Transformer</tt> name. The name is used to store
     * <tt>Transformer</tt> associated data.
     * 
     * @return The <tt>Transformer</tt> name.
     */
    String getName();

    /**
     * Transforms an input data to some custom representation.
     * Input and output are not passed implicitly, which means that
     * <tt>Transformer</tt> is able to retrieve input and output from its
     * internal state or from external storage ({@link AttributeStorage}).
     * 
     * @param storage the external state storage, where <tt>Transformer</tt> could
     *        get/put a state.
     * @param input data to transform
     * @return the result {@link TransformationResult}
     * 
     * @throws org.glassfish.grizzly.TransformationException if failed to transport i.e. invalid types
     */
    TransformationResult<K, L> transform(AttributeStorage storage, K input)
            throws TransformationException;

    /**
     * Gets the last returned <tt>Transformer</tt> result.
     * Last result could be either retrieved from internal state, or external
     * storage, which is passed as the parameter.
     * 
     * @param storage the external state storage, where <tt>Transformer</tt>
     *        could retrieve or store its state.
     * @return the last returned <tt>Transformer</tt> result.
     */
    TransformationResult<K, L> getLastResult(AttributeStorage storage);

    /**
     * The <tt>Transformer</tt> has done its work and can release all
     * associated resource.
     *
     * @param storage the external state storage, where <tt>Transformer</tt>
     *        could retrieve or store its state.
     */
    void release(AttributeStorage storage);

    boolean hasInputRemaining(AttributeStorage storage, K input);
}
