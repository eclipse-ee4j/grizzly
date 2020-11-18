/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.simpleauth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Packet, which contains multiple String lines.
 *
 * @author Alexey Stashok
 */
public class MultiLinePacket {
    // String lines list
    private final List<String> lines;

    public static MultiLinePacket create() {
        return new MultiLinePacket();
    }

    public static MultiLinePacket create(String... lines) {
        final MultiLinePacket packet = new MultiLinePacket();
        packet.getLines().addAll(Arrays.asList(lines));

        return packet;
    }

    static MultiLinePacket create(List<String> lines) {
        return new MultiLinePacket(lines);
    }

    private MultiLinePacket() {
        lines = new ArrayList<>();
    }

    private MultiLinePacket(List<String> lines) {
        this.lines = lines;
    }

    /**
     * Gets the packet string lines.
     * 
     * @return the packet string lines.
     */
    public List<String> getLines() {
        return lines;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);

        for (String line : lines) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MultiLinePacket && lines.equals(((MultiLinePacket) obj).lines);

    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.lines != null ? this.lines.hashCode() : 0);
        return hash;
    }
}
