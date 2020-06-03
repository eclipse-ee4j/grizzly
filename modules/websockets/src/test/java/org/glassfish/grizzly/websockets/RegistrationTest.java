/*
 * Copyright (c) 2015, 2020 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.ProcessingState;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test websocket application registration process.
 *
 * @author Grizzly team
 */
public class RegistrationTest {
    @Test
    public void testGRIZZLY1765() throws Exception {
        final WebSocketApplication applicationA = new WebSocketApplication() {
        };
        final WebSocketApplication applicationB = new WebSocketApplication() {
        };

        HttpRequestPacket request = new HttpRequestPacket() {
            @Override
            public ProcessingState getProcessingState() {
                return null;
            }
        };
        request.setRequestURI("/sample");

        WebSocketEngine.getEngine().register("", "/sample", applicationA);
        WebSocketEngine.getEngine().register("", "/sample", applicationB);
        WebSocketEngine.getEngine().unregister(applicationA);
        Assert.assertEquals(applicationB, WebSocketEngine.getEngine().getApplication(request));
        WebSocketEngine.getEngine().unregister(applicationB);
        Assert.assertNull(WebSocketEngine.getEngine().getApplication(request));
    }
}
