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

import org.glassfish.grizzly.Connection;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import org.glassfish.grizzly.Grizzly;

/**
 *
 * @author oleksiys
 */
public class SSLSupportImpl implements SSLSupport {
    private static final Logger logger = Grizzly.logger(SSLSupportImpl.class);

    /**
     * A mapping table to determine the number of effective bits in the key
     * when using a cipher suite containing the specified cipher name.  The
     * underlying data came from the TLS Specification (RFC 2246), Appendix C.
     */
    private static final CipherData ciphers[] = {
            new CipherData("_WITH_NULL_", 0),
            new CipherData("_WITH_IDEA_CBC_", 128),
            new CipherData("_WITH_RC2_CBC_40_", 40),
            new CipherData("_WITH_RC4_40_", 40),
            new CipherData("_WITH_RC4_128_", 128),
            new CipherData("_WITH_DES40_CBC_", 40),
            new CipherData("_WITH_DES_CBC_", 56),
            new CipherData("_WITH_3DES_EDE_CBC_", 168),
            new CipherData("_WITH_AES_128_", 128),
            new CipherData("_WITH_AES_256_", 256)
    };
    
    public static final String KEY_SIZE_KEY = "SSL_KEY_SIZE";
    private final SSLEngine engine;
    private volatile SSLSession session;

    public SSLSupportImpl(Connection connection) {
        
        engine = SSLUtils.getSSLEngine(connection);
        if (engine == null) {
            throw new IllegalStateException("SSLEngine is null");
        }
        session = engine.getSession();
    }


    @Override
    public String getCipherSuite() throws IOException {
        // Look up the current SSLSession
                /* SJSAS 6439313
         * SSLSession session = ssl.getSession();
         */
        if (session == null) {
            return null;
        }
        return session.getCipherSuite();
    }

    @Override
    public Object[] getPeerCertificateChain() throws IOException {
        return getPeerCertificateChain(false);
    }

    protected X509Certificate[] getX509Certificates(
            SSLSession session) throws IOException {
        X509Certificate[] jsseCerts = null;
        try {
            jsseCerts = (X509Certificate[])session.getPeerCertificates();
        } catch (Throwable ex) {
            // Get rid of the warning in the logs when no Client-Cert is
            // available
        }

        if (jsseCerts == null) {
            jsseCerts = new X509Certificate[0];
        }

        if (jsseCerts.length < 1) {
            return null;
        }
        return jsseCerts;
    }

    @Override
    public Object[] getPeerCertificateChain(boolean force)
            throws IOException {
        // Look up the current SSLSession
        /* SJSAS 6439313
        SSLSession session = ssl.getSession();
         */
        if (session == null) {
            return null;
        }

        // Convert JSSE's certificate format to the ones we need
        Certificate[] jsseCerts = null;
        try {
            jsseCerts = session.getPeerCertificates();
        } catch (Exception bex) {
            // ignore.
        }
        if (jsseCerts == null) {
            jsseCerts = new Certificate[0];
        }
        if (jsseCerts.length <= 0 && force) {
            session.invalidate();
//            handshaker.handshake(connection, engineConfigurator);
            /* SJSAS 6439313
            session = ssl.getSession();
             */
            // START SJSAS 6439313
            session = engine.getSession();
        // END SJSAS 6439313
        }
        return getX509Certificates(session);
    }

    /**
     * Copied from <code>org.apache.catalina.valves.CertificateValve</code>
     */
    @Override
    public Integer getKeySize()
            throws IOException {
        // Look up the current SSLSession
        /* SJSAS 6439313
        SSLSession session = ssl.getSession();
         */
        SSLSupport.CipherData c_aux[] = ciphers;
        if (session == null) {
            return null;
        }
        Integer keySize = (Integer) session.getValue(KEY_SIZE_KEY);
        if (keySize == null) {
            int size = 0;
            String cipherSuite = session.getCipherSuite();

            for (int i = 0; i < c_aux.length; i++) {
                if (cipherSuite.contains(c_aux[i].phrase)) {
                    size = c_aux[i].keySize;
                    break;
                }
            }
            keySize = size;
            session.putValue(KEY_SIZE_KEY, keySize);
        }
        return keySize;
    }

    @Override
    public String getSessionId() throws IOException {
        // Look up the current SSLSession
                /* SJSAS 6439313
         * SSLSession session = ssl.getSession();
         */
        if (session == null) {
            return null;
        }
        // Expose ssl_session (getId)
        byte[] ssl_session = session.getId();
        if (ssl_session == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder("");
        for (int x = 0; x < ssl_session.length; x++) {
            String digit = Integer.toHexString((int) ssl_session[x]);
            if (digit.length() < 2) {
                buf.append('0');
            }
            if (digit.length() > 2) {
                digit = digit.substring(digit.length() - 2);
            }
            buf.append(digit);
        }
        return buf.toString();
    }
}
