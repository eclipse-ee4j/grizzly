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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.filterchain.FilterChainEvent;

/**
 * The class contains a set of standard HTTP events sent on FilterChain during
 * HTTP packets processing.
 * 
 * @author Alexey Stashok
 */
public class HttpEvents {
    public static IncomingHttpUpgradeEvent createIncomingUpgradeEvent(
            final HttpHeader httpHeader) {
        return new IncomingHttpUpgradeEvent(httpHeader);
    }
    
    public static OutgoingHttpUpgradeEvent createOutgoingUpgradeEvent(
            final HttpHeader httpHeader) {
        return new OutgoingHttpUpgradeEvent(httpHeader);
    }

    public static ChangePacketInProgressEvent createChangePacketInProgressEvent(
            final HttpHeader packet) {
        return new ChangePacketInProgressEvent(packet);
    }
    
    public static final class ChangePacketInProgressEvent implements FilterChainEvent {
        public static final Object TYPE = ChangePacketInProgressEvent.class.getName();
        
        private final HttpHeader httpHeader;

        private ChangePacketInProgressEvent(HttpHeader httpHeader) {
            this.httpHeader = httpHeader;
        }
        
        public HttpHeader getPacket() {
            return httpHeader;
        }

        @Override
        public Object type() {
            return TYPE;
        }
    }

    public static final class ResponseCompleteEvent implements FilterChainEvent {
        public static final Object TYPE = ResponseCompleteEvent.class.getName();
        
        @Override
        public Object type() {
            return TYPE;
        }
        
    }
    
    public static final class IncomingHttpUpgradeEvent extends HttpUpgradeEvent {
        public static final Object TYPE = IncomingHttpUpgradeEvent.class.getName();

        private IncomingHttpUpgradeEvent(final HttpHeader httpHeader) {
            super(httpHeader);
        }
        
        @Override
        public Object type() {
            return TYPE;
        }
    }
    
    public static final class OutgoingHttpUpgradeEvent extends HttpUpgradeEvent {
        public static final Object TYPE = OutgoingHttpUpgradeEvent.class.getName();

        private OutgoingHttpUpgradeEvent(final HttpHeader httpHeader) {
            super(httpHeader);
        }
        
        @Override
        public Object type() {
            return TYPE;
        }
    }

    private static abstract class HttpUpgradeEvent implements FilterChainEvent {
        private final HttpHeader httpHeader;
        
        private HttpUpgradeEvent(final HttpHeader httpHeader) {
            this.httpHeader = httpHeader;
        }

        public HttpHeader getHttpHeader() {
            return httpHeader;
        }
    }
}
