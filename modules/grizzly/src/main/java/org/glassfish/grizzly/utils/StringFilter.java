/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.filterchain.AbstractCodecFilter;
import java.nio.charset.Charset;

/**
 * StringFilter implementation, which performs Buffer <-> String transformation.
 * 
 * @author Alexey Stashok
 */
public final class StringFilter extends AbstractCodecFilter<Buffer, String> {

    public StringFilter() {
        this(null, null);
    }
    
    public StringFilter(final Charset charset) {
        this(charset, null);
    }

    public StringFilter(final Charset charset, final String stringTerminatingSymb) {
        super(new StringDecoder(charset, stringTerminatingSymb),
                new StringEncoder(charset, stringTerminatingSymb));
    }
}
