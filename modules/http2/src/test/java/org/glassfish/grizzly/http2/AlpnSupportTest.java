package org.glassfish.grizzly.http2;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.npn.NegotiationSupport;
import org.glassfish.grizzly.ssl.SSLUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AlpnSupportTest extends AbstractHttp2Test {

    public static final int PORT = 18894;
    private HttpServer server;
    private TCPNIOTransport clientTransport;

    @Before
    public void init() throws IOException {
        server = createServer(null, PORT, true);
        clientTransport = TCPNIOTransportBuilder.newInstance().setProcessor(createClientFilterChain(true)).build();

        server.start();
        clientTransport.start();
    }

    @After
    public void destroy() throws IOException {
        clientTransport.shutdownNow();
        server.shutdownNow();
    }

    /**
     * Tests that the SSL_TO_CONNECTION_MAP removes entries from it after the
     * connection closes.
     */
    @Test
    public void sslToConnectionMapClearTest() throws Exception {
        SSLEngine engine = null;

        Connection<?> connection = null;
        try {
            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            connection = connectFuture.get(10, TimeUnit.SECONDS);
            assertNotNull("Failed to get connection to server.", connection);
            engine = SSLUtils.getSSLEngine(connection);
        } finally {
            // Close the client connection
            if (connection != null) {
                connection.closeSilently();
            }
        }

        // Perform a garbage collection and wait for 10 seconds to see if the engine reference clears
        System.gc();
        for (int i = 0; i < 10; i++) {
            if (AlpnSupport.getConnection(engine) == null) {
                break;
            }
            System.gc();
            Thread.sleep(1000);
        }

        // Check the connection is gone
        assertNull("The SSLEngine was left hanging in the ALPN map.", AlpnSupport.getConnection(engine));
        assertNull("The SSLEngine was left hanging in the negotiation support.", NegotiationSupport.getAlpnClientNegotiator(engine));
        assertNull("The SSLEngine was left hanging in the negotiation support.", NegotiationSupport.getAlpnServerNegotiator(engine));
        assertNull("The SSLEngine was left hanging in the negotiation support.", NegotiationSupport.getClientSideNegotiator(engine));
        assertNull("The SSLEngine was left hanging in the negotiation support.", NegotiationSupport.getServerSideNegotiator(engine));
    }
}
