/*
 * Copyright (c) 2010, 2017 Oracle and/or its affiliates. All rights reserved.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import org.glassfish.grizzly.utils.Charsets;

/**
 * Class represents {@link WebSocket}'s security key, used during the handshake phase.
 *
 * @author Alexey Stashok
 */
public class SecKey {
    private static final Random random = new SecureRandom();

    public static final int KEY_SIZE = 16;

    /**
     * Security key string representation, which includes chars and spaces.
     */
    private final String secKey;

    private byte[] bytes;

    public SecKey() {
        secKey = create();
    }

    private String create() {
        bytes = new byte[KEY_SIZE];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public SecKey(String base64) {
        if(base64 == null) {
            throw new HandshakeException("Null keys are not allowed.");
        }
        secKey = base64;
    }

    /**
     * Generate server-side security key, which gets passed to the client during
     * the handshake phase as part of message payload.
     *
     * @param clientKey client's Sec-WebSocket-Key
     * @return server key.
     *
     */
    public static SecKey generateServerKey(SecKey clientKey) throws HandshakeException {
        String key = clientKey.getSecKey() + Constants.SERVER_KEY_HASH;
        final MessageDigest instance;
        try {
            instance = MessageDigest.getInstance("SHA-1");
            instance.update(key.getBytes(Charsets.ASCII_CHARSET));
            final byte[] digest = instance.digest();
            if(digest.length != 20) {
                throw new HandshakeException("Invalid key length.  Should be 20: " + digest.length);
            }

            return new SecKey(Base64.getEncoder().encodeToString(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new HandshakeException(e.getMessage());
        }
    }

    /**
     * Gets security key string representation, which includes chars and spaces.
     *
     * @return Security key string representation, which includes chars and spaces.
     */
    public String getSecKey() {
        return secKey;
    }

    @Override
    public String toString() {
        return secKey;
    }

    public byte[] getBytes() {
        if(bytes == null) {
            bytes = Base64.getDecoder().decode(secKey);
        }
        return bytes;
    }

    public void validateServerKey(String serverKey) {
        final SecKey key = generateServerKey(this);
        if(!key.getSecKey().equals(serverKey)) {
            throw new HandshakeException("Server key returned does not match expected response");
        }
    }
}
