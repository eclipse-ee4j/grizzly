/*
 * Copyright (c) 2011, 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.jaxws.service;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

/**
 * Simple "add" web service implementation.
 * 
 * @author Alexey Stashok
 */
@WebService
public class AddService {
    @WebMethod
    public int add(@WebParam(name="value1") int value1, @WebParam(name="value2") int value2) {
        return value1 + value2;
    }
}
