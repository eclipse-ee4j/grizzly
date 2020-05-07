/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.grizzly.samples.jaxws.addclient;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for add complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="add">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="value1" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="value2" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "add", propOrder = {
    "value1",
    "value2"
})
public class Add {

    protected int value1;
    protected int value2;

    /**
     * Gets the value of the value1 property.
     * 
     */
    public int getValue1() {
        return value1;
    }

    /**
     * Sets the value of the value1 property.
     * 
     */
    public void setValue1(int value) {
        this.value1 = value;
    }

    /**
     * Gets the value of the value2 property.
     * 
     */
    public int getValue2() {
        return value2;
    }

    /**
     * Sets the value of the value2 property.
     * 
     */
    public void setValue2(int value) {
        this.value2 = value;
    }

}
