/*
 * Copyright (c) 2015, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.ajp;

import java.nio.ByteBuffer;

public class AjpShutdownPacket extends AjpPacket {
    private final String secretKey;

    public AjpShutdownPacket(final String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    protected ByteBuffer buildContent() {
        ByteBuffer header = ByteBuffer.allocate(2);
        header.put(AjpConstants.JK_AJP13_SHUTDOWN);
        if (secretKey != null) {
            header = putString(header, secretKey);
        }
        
        header.flip();

        return header;
    }
}
