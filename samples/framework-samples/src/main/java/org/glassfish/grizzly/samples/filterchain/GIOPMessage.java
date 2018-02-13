/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.filterchain;

import java.util.Arrays;


/**
 * Simple representation of GIOP message
 *
 * @author Alexey Stashok
 */
public class GIOPMessage {
    private byte G;
    private byte I;
    private byte O;
    private byte P;

    private byte major;
    private byte minor;

    private byte flags;
    private byte value;

    private int bodyLength;

    private byte[] body;

    public GIOPMessage() {
    }

    public GIOPMessage(byte major, byte minor,
            byte flags, byte value, byte[] body) {
        G = 'G';
        I = 'I';
        O = 'O';
        P = 'P';

        this.major = major;
        this.minor = minor;
        this.flags = flags;
        this.value = value;
        
        bodyLength = body.length;
        this.body = body;
    }

    public byte[] getGIOPHeader() {
        byte[] giopHeader = new byte[4];
        giopHeader[0] = G;
        giopHeader[1] = I;
        giopHeader[2] = O;
        giopHeader[3] = P;

        return giopHeader;
    }

    public void setGIOPHeader(byte G, byte I, byte O, byte P) {
        this.G = G;
        this.I = I;
        this.O = O;
        this.P = P;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public byte getMajor() {
        return major;
    }

    public void setMajor(byte major) {
        this.major = major;
    }

    public byte getMinor() {
        return minor;
    }

    public void setMinor(byte minor) {
        this.minor = minor;
    }

    public byte getValue() {
        return value;
    }

    public void setValue(byte value) {
        this.value = value;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GIOPMessage other = (GIOPMessage) obj;
        if (this.G != other.G) {
            return false;
        }
        if (this.I != other.I) {
            return false;
        }
        if (this.O != other.O) {
            return false;
        }
        if (this.P != other.P) {
            return false;
        }
        if (this.major != other.major) {
            return false;
        }
        if (this.minor != other.minor) {
            return false;
        }
        if (this.flags != other.flags) {
            return false;
        }
        if (this.value != other.value) {
            return false;
        }
        if (this.bodyLength != other.bodyLength) {
            return false;
        }
        if (this.body != other.body && (this.body == null ||
                !Arrays.equals(this.body, other.body))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.G;
        hash = 97 * hash + this.I;
        hash = 97 * hash + this.O;
        hash = 97 * hash + this.P;
        hash = 97 * hash + this.major;
        hash = 97 * hash + this.minor;
        hash = 97 * hash + this.flags;
        hash = 97 * hash + this.value;
        hash = 97 * hash + this.bodyLength;
        hash = 97 * hash + (this.body != null ? Arrays.hashCode(this.body) : 0);
        return hash;
    }
}
