/*
 * Copyright (c) 2009, 2017 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.ssl;

import java.io.IOException;

/**
 * SSLSupport
 *
 * Interface for SSL-specific functions
 *
 * @author EKR
 */
public interface SSLSupport {

    /**
     * The Request attribute key for the cipher suite.
     */
    String CIPHER_SUITE_KEY = "jakarta.servlet.request.cipher_suite";
    /**
     * The Request attribute key for the key size.
     */
    String KEY_SIZE_KEY = "jakarta.servlet.request.key_size";
    /**
     * The Request attribute key for the client certificate chain.
     */
    String CERTIFICATE_KEY = "jakarta.servlet.request.X509Certificate";
    /**
     * The Request attribute key for the session id.
     * This one is a Tomcat extension to the Servlet spec.
     */
    String SESSION_ID_KEY = "jakarta.servlet.request.ssl_session_id";

    /**
     * The cipher suite being used on this connection.
     */
    String getCipherSuite() throws IOException;

    /**
     * The client certificate chain (if any).
     */
    Object[] getPeerCertificateChain()
            throws IOException;

    /**
     * The client certificate chain (if any).
     * @param force If <tt>true</tt>, then re-negotiate the
     *              connection if necessary.
     */
    Object[] getPeerCertificateChain(boolean force)
            throws IOException;

    /**
     * Get the keysize.
     *
     * What we're supposed to put here is ill-defined by the
     * Servlet spec (S 4.7 again). There are at least 4 potential
     * values that might go here:
     *
     * (a) The size of the encryption key
     * (b) The size of the MAC key
     * (c) The size of the key-exchange key
     * (d) The size of the signature key used by the server
     *
     * Unfortunately, all of these values are nonsensical.
     **/
    Integer getKeySize()
            throws IOException;

    /**
     * The current session Id.
     */
    String getSessionId()
            throws IOException;

    /**
     * Simple data class that represents the cipher being used, along with the
     * corresponding effective key size.  The specified phrase must appear in the
     * name of the cipher suite to be recognized.
     */
    final class CipherData {

        public String phrase = null;
        public int keySize = 0;

        public CipherData(String phrase, int keySize) {
            this.phrase = phrase;
            this.keySize = keySize;
        }
    }
}
