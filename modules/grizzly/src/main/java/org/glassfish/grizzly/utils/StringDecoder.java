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
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.TransformationException;
import org.glassfish.grizzly.TransformationResult;
import org.glassfish.grizzly.attributes.Attribute;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * String decoder, which decodes {@link Buffer} to {@link String}
 * 
 * @author Alexey Stashok
 */
public class StringDecoder extends AbstractTransformer<Buffer, String> {
    private static final Logger logger = Grizzly.logger(StringDecoder.class);
    
    protected Charset charset;
    
    protected final Attribute<Integer> lengthAttribute;

    protected byte[] stringTerminateBytes;

    @SuppressWarnings("unused")
    public StringDecoder() {
        this(null, null);
    }

    @SuppressWarnings("unused")
    public StringDecoder(final String stringTerminator) {
        this(Charset.forName("UTF-8"), stringTerminator);
    }

    public StringDecoder(final Charset charset) {
        this(charset, null);
    }

    public StringDecoder(final Charset charset, final String stringTerminator) {
        this.charset = charset != null ? charset : Charset.defaultCharset();
        
        if (stringTerminator != null) {
            try {
                this.stringTerminateBytes = stringTerminator.getBytes(
                        this.charset.name());
            } catch (final UnsupportedEncodingException ignored) {
                // should never happen as we are getting charset name from Charset
            }
        }

        lengthAttribute = attributeBuilder.createAttribute(
                "StringDecoder.StringSize");
    }

    @Override
    public String getName() {
        return "StringDecoder";
    }

    @Override
    protected TransformationResult<Buffer, String> transformImpl(
            final AttributeStorage storage, final Buffer input)
            throws TransformationException {

        if (input == null) {
            throw new TransformationException("Input could not be null");
        }

        final TransformationResult<Buffer, String> result;

        result = stringTerminateBytes == null
                ? parseWithLengthPrefix(storage, input)
                : parseWithTerminatingSeq(storage, input);

        return result;
    }

    protected TransformationResult<Buffer, String> parseWithLengthPrefix(
            final AttributeStorage storage, final Buffer input) {
        Integer stringSize = lengthAttribute.get(storage);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "StringDecoder decode stringSize={0} buffer={1} content={2}",
                    new Object[]{stringSize, input, input.toStringContent()});
        }

        if (stringSize == null) {
            if (input.remaining() < 4) {
                return TransformationResult.createIncompletedResult(input);
            }

            stringSize = input.getInt();
            lengthAttribute.set(storage, stringSize);
        }
        
        if (input.remaining() < stringSize) {
            return TransformationResult.createIncompletedResult(input);
        }

        final int tmpLimit = input.limit();
        input.limit(input.position() + stringSize);
        final String stringMessage = input.toStringContent(charset);
        input.position(input.limit());
        input.limit(tmpLimit);

        return TransformationResult.createCompletedResult(
                stringMessage, input);
    }

    protected TransformationResult<Buffer, String> parseWithTerminatingSeq(
            final AttributeStorage storage, final Buffer input) {
        final int terminationBytesLength = stringTerminateBytes.length;
        int checkIndex = 0;
        
        int termIndex = -1;

        final Integer offsetInt = lengthAttribute.get(storage);
        int offset = 0;
        if (offsetInt != null) {
            offset = offsetInt;
        }

        for (int i = input.position() + offset, lim = input.limit(); i < lim; i++) {
            if (input.get(i) == stringTerminateBytes[checkIndex]) {
                checkIndex++;
                if (checkIndex >= terminationBytesLength) {
                    termIndex = i - terminationBytesLength + 1;
                    break;
                }
            }
        }

        if (termIndex >= 0) {
            // Terminating sequence was found
            final int tmpLimit = input.limit();
            input.limit(termIndex);
            final String stringMessage = input.toStringContent(charset);
            input.limit(tmpLimit);
            input.position(termIndex + terminationBytesLength);
            return TransformationResult.createCompletedResult(
                    stringMessage, input);
        } else {
            offset = input.remaining() - terminationBytesLength;
            if (offset < 0) {
                offset = 0;
            }

            lengthAttribute.set(storage, offset);            
            return TransformationResult.createIncompletedResult(
                    input);
        }
    }

    @Override
    public void release(final AttributeStorage storage) {
        lengthAttribute.remove(storage);
        super.release(storage);
    }

    @Override
    public boolean hasInputRemaining(final AttributeStorage storage,
                                     final Buffer input) {
        return input != null && input.hasRemaining();
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(final Charset charset) {
        this.charset = charset;
    }
}
