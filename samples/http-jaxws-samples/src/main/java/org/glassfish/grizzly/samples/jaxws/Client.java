/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.jaxws;

import org.glassfish.grizzly.samples.jaxws.addclient.AddService;
import org.glassfish.grizzly.samples.jaxws.addclient.AddServiceService;

/**
 * Simple web service client, which uses auto-generated client classes and executes "add" operation.
 *
 * @author Alexey Stashok
 */
public class Client {
    public static void main(String[] args) {
        // Standard web service call
        AddServiceService service = new AddServiceService();
        AddService port = service.getAddServicePort();

        final int value1 = 2;
        final int value2 = 3;

        final int result = port.add(value1, value2);

        System.out.println(value1 + "+" + value2 + "=" + result);
    }
}
