/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.attributes.AttributeStorage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import org.glassfish.grizzly.AbstractTransformer;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;

/**
 * String decoder, which decodes {@link Buffer} to {@link String}
 *
 * @author Alexey Stashok
 */
public class StringEncoder extends AbstractTransformer<String, Buffer> {

    protected Charset charset;

    protected String stringTerminator;

    public StringEncoder() {
        this((String) null);
    }

    public StringEncoder(final String stringTerminator) {
        this(null, stringTerminator);
    }

    @SuppressWarnings("unused")
    public StringEncoder(final Charset charset) {
        this(charset, null);
    }

    public StringEncoder(final Charset charset, final String stringTerminator) {
        this.charset = charset != null ? charset : Charset.defaultCharset();

        this.stringTerminator = stringTerminator;
    }

    @Override
    public String getName() {
        return "StringEncoder";
    }

    @Override
    protected TransformationResult<String, Buffer> transformImpl(
            final AttributeStorage storage, String input)
            throws TransformationException {

        if (input == null) {
            throw new TransformationException("Input could not be null");
        }

        final byte[] byteRepresentation;
        try {
            if (stringTerminator != null) {
                input = input + stringTerminator;
            }
            
            byteRepresentation = input.getBytes(charset.name());
        } catch(final UnsupportedEncodingException e) {
            throw new TransformationException("Charset " +
                    charset.name() + " is not supported", e);
        }

        final Buffer output =
                obtainMemoryManager(storage).allocate(byteRepresentation.length + 4);

        if (stringTerminator == null) {
            output.putInt(byteRepresentation.length);
        }

        output.put(byteRepresentation);

        output.flip();
        output.allowBufferDispose(true);

        return TransformationResult.createCompletedResult(output, null);
    }

    @Override
    public boolean hasInputRemaining(final AttributeStorage storage,
                                     final String input) {
        return input != null;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(final Charset charset) {
        this.charset = charset;
    }
}
