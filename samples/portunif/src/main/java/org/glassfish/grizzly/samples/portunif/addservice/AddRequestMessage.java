/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.portunif.addservice;

/**
 * ADD-service request message
 * 
 * @author Alexey Stashok
 */
public class AddRequestMessage {
    private final int value1;
    private final int value2;

    /**
     * Construct Request message
     * 
     * @param value1
     * @param value2
     */
    public AddRequestMessage(int value1, int value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    /**
     * Get value1
     * @return value1
     */
    public int getValue1() {
        return value1;
    }

    /**
     * Get value2
     * @return value2
     */
    public int getValue2() {
        return value2;
    }
}
