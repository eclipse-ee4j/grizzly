/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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
public class AddResponseMessage {
    private final int result;

    /**
     * Construct Response message
     *
     * @param result
     */
    public AddResponseMessage(int result) {
        this.result = result;
    }

    /**
     * Get result
     *
     * @return result
     */
    public int getResult() {
        return result;
    }
}
