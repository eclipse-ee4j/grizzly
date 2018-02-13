/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.websockets.frametypes;

import org.glassfish.grizzly.websockets.BaseFrameType;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.FrameType;
import org.glassfish.grizzly.websockets.WebSocket;

public class ContinuationFrameType extends BaseFrameType {
    private final boolean text;
    private final FrameType wrappedType;

    public ContinuationFrameType(boolean text) {
        this.text = text;
        wrappedType = text ? new TextFrameType() : new BinaryFrameType();
    }

    public void respond(WebSocket socket, DataFrame frame) {
        if (text) {
            socket.onFragment(frame.isLast(), frame.getTextPayload());
        } else {
            socket.onFragment(frame.isLast(), frame.getBytes());
        }
    }

    @Override
    public void setPayload(DataFrame frame, byte[] data) {
        wrappedType.setPayload(frame, data);
    }

    @Override
    public byte[] getBytes(DataFrame dataFrame) {
        return wrappedType.getBytes(dataFrame);
    }
}
