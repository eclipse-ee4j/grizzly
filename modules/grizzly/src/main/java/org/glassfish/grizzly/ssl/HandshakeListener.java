package org.glassfish.grizzly.ssl;

import javax.net.ssl.SSLEngine;

import org.glassfish.grizzly.Connection;

public interface HandshakeListener {
    void onInit(Connection<?> connection, SSLEngine sslEngine);
    void onStart(Connection<?> connection);
    void onComplete(Connection<?> connection);
    void onFailure(Connection<?> connection, Throwable t);
}
