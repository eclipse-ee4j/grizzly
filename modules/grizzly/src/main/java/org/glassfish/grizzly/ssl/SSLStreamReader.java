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

package org.glassfish.grizzly.ssl;

import org.glassfish.grizzly.streams.TransformerStreamReader;
import org.glassfish.grizzly.streams.StreamReader;

/**
 * SSL aware {@link StreamReader} implementation, which work like a wrapper over
 * existing {@link StreamReader}.
 *
 * @see SSLStreamWriter
 * 
 * @author Alexey Stashok
 */
public class SSLStreamReader extends TransformerStreamReader {
    public SSLStreamReader(StreamReader underlyingReader) {
        super(underlyingReader, new SSLDecoderTransformer());
    }
}
