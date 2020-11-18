/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets;

public class Utf8DecodingError extends FramingException {

    public Utf8DecodingError(String s) {
        super(s);
    }

    public Utf8DecodingError(String s, Throwable throwable) {
        super(s, throwable);
    }

    public Utf8DecodingError(Throwable throwable) {
        super(throwable);
    }

    @Override
    public int getClosingCode() {
        return 1007;
    }

}
