/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.filterchain.TransportFilter;
import org.glassfish.grizzly.http.Cookie;
import org.glassfish.grizzly.http.Cookies;
import org.glassfish.grizzly.http.HttpClientFilter;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.Protocol;
import org.glassfish.grizzly.http.server.util.Globals;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.MimeHeaders;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder;
import org.glassfish.grizzly.utils.ChunkingFilter;

import junit.framework.TestCase;

/**
 * Session parsing tests
 *
 * @author Alexey Stashok
 */
@SuppressWarnings("unchecked")
public class HttpSessionTest extends TestCase {
    private static int PORT = PORT();

    static int PORT() {
        try {
            int port = 8039 + SecureRandom.getInstanceStrong().nextInt(1000);
            System.out.println("Using port: " + port);
            return port;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void testPassedSessionId() throws Exception {
        HttpHandler httpHandler = new HttpSessionHandler();
        HttpPacket request = createRequest("/index.html;jsessionid=123456", null);
        HttpContent response = doTest(httpHandler, request, 10);

        String responseContent = response.getContent().toStringContent();
        Map<String, String> props = new HashMap<>();

        BufferedReader reader = new BufferedReader(new StringReader(responseContent));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] nameValue = line.split("=");
            assertEquals(2, nameValue.length);
            props.put(nameValue[0], nameValue[1]);
        }

        String sessionId = props.get("session-id");
        assertNotNull(sessionId);
        assertEquals("123456", sessionId);
    }

    public void testPassedSessionId2() throws Exception {
        HttpHandler httpHandler = new HttpSessionHandler();
        HttpPacket request = createRequest("/index.html;jsessionid=123456;var=abc", null);
        HttpContent response = doTest(httpHandler, request, 10);

        String responseContent = response.getContent().toStringContent();
        Map<String, String> props = new HashMap<>();

        BufferedReader reader = new BufferedReader(new StringReader(responseContent));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] nameValue = line.split("=");
            assertEquals(2, nameValue.length);
            props.put(nameValue[0], nameValue[1]);
        }

        String sessionId = props.get("session-id");
        assertNotNull(sessionId);
        assertEquals("123456", sessionId);
    }

    public void testPassedSessionIdAndJRoute() throws Exception {
        HttpHandler httpHandler = new HttpSessionHandler();
        HttpPacket request = createRequest("/index.html;jsessionid=123456:987", null);
        HttpContent response = doTest(httpHandler, request, 10);

        String responseContent = response.getContent().toStringContent();
        Map<String, String> props = new HashMap<>();

        BufferedReader reader = new BufferedReader(new StringReader(responseContent));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] nameValue = line.split("=");
            assertEquals(2, nameValue.length);
            props.put(nameValue[0], nameValue[1]);
        }

        String sessionId = props.get("session-id");
        assertNotNull(sessionId);
        assertEquals("123456", sessionId);

        String jrouteId = props.get("jroute-id");
        assertNotNull(jrouteId);
        assertEquals("987", jrouteId);

    }

    public void testCreateSession() throws Exception {
        HttpHandler httpHandler = new HttpCreaeteSessionHandler();
        HttpPacket request = createRequest("/session", null);
        HttpContent response = doTest(httpHandler, request, 10);

        String responseContent = response.getContent().toStringContent();
        Map<String, String> props = new HashMap<>();

        String cookieSessionId = null;
        int sessionCookiesNum = 0;
        Iterable<String> it = response.getHttpHeader().getHeaders().values("Set-Cookie");
        for (String value : it) {
            sessionCookiesNum++;
            cookieSessionId = value;
        }

        assertEquals(1, sessionCookiesNum);

        // Check session-id in the content
        BufferedReader reader = new BufferedReader(new StringReader(responseContent));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] nameValue = line.split("=");
            assertEquals(2, nameValue.length);
            props.put(nameValue[0], nameValue[1]);
        }

        String sessionId = props.get("session-id");
        assertNotNull(sessionId);
        assertTrue(cookieSessionId.contains(sessionId));
    }

    public void testCreateSessionWithInvalidId() throws Exception {
        HttpHandler httpHandler = new HttpCreaeteSessionHandler();
        HttpPacket request = createRequest("/session;jsessionid=123456", null);
        HttpContent response = doTest(httpHandler, request, 10);

        String responseContent = response.getContent().toStringContent();
        Map<String, String> props = new HashMap<>();

        String cookieSessionId = null;
        int sessionCookiesNum = 0;
        Iterable<String> it = response.getHttpHeader().getHeaders().values("Set-Cookie");
        for (String value : it) {
            sessionCookiesNum++;
            cookieSessionId = value;
        }

        assertEquals(1, sessionCookiesNum);

        // Check session-id in the content
        BufferedReader reader = new BufferedReader(new StringReader(responseContent));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] nameValue = line.split("=");
            assertEquals(2, nameValue.length);
            props.put(nameValue[0], nameValue[1]);
        }

        String sessionId = props.get("session-id");
        assertNotNull(sessionId);
        assertTrue(cookieSessionId.contains(sessionId));
        assertFalse("123456".equals(sessionId));
    }

    public void testChangeSessionId() throws Exception {
        HttpHandler httpHandler = new HttpHandler() {
            @Override
            public void service(Request req, Response res) throws Exception {
                Session session = req.getSession(false);
                if (session == null) {
                    req.getSession(true).setAttribute("A", "1");
                } else {
                    session.setAttribute("A", "2");
                    req.changeSessionId();
                }
                Object a = req.getSession(false).getAttribute("A");
                res.addHeader("A", (String) a);
            }
        };

        HttpServer server = createWebServer(httpHandler);

        try {
            Thread.sleep(10);
            server.start();
            HttpPacket request1 = createRequest("/test", null);
            HttpContent response1 = sendRequest(request1, 10);

            Cookie[] cookies1 = getCookies(response1.getHttpHeader().getHeaders());

            assertEquals(1, cookies1.length);
            assertEquals(Globals.SESSION_COOKIE_NAME, cookies1[0].getName());

            String[] values1 = getHeaderValues(response1.getHttpHeader().getHeaders(), "A");
            assertEquals(1, values1.length);
            assertEquals("1", values1[0]);

            HttpPacket request2 = createRequest("/test",
                    Collections.singletonMap(Header.Cookie.toString(), Globals.SESSION_COOKIE_NAME + "=" + cookies1[0].getValue()));

            HttpContent response2 = sendRequest(request2, 10);
            Cookie[] cookies2 = getCookies(response2.getHttpHeader().getHeaders());

            assertEquals(1, cookies2.length);
            assertEquals(Globals.SESSION_COOKIE_NAME, cookies2[0].getName());

            String[] values2 = getHeaderValues(response2.getHttpHeader().getHeaders(), "A");
            assertEquals(1, values2.length);
            assertEquals("2", values2[0]);

            assertTrue(!cookies1[0].getValue().equals(cookies2[0].getValue()));

        } finally {
            server.shutdownNow();
        }
    }

    public void testEncodeURL() throws Exception {

        HttpServer server = createWebServer(new HttpEncodeURLHandler());

        try {
            Thread.sleep(10);
            server.start();

            HttpPacket request = createRequest("/index.html", null);
            HttpContent response = sendRequest(request, 10);

            String responseContent = response.getContent().toStringContent();
            Map<String, String> props = new HashMap<>();

            BufferedReader reader = new BufferedReader(new StringReader(responseContent));
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                assertTrue(idx != -1);

                props.put(line.substring(0, idx), line.substring(idx + 1, line.length()));
            }
            System.out.println(props);

            String sessionId = props.get("session-id");
            assertNotNull(sessionId);

            String encodeURL1 = props.get("encodeURL1");
            assertEquals("/encodeURL", encodeURL1);

            String encodeURL2 = props.get("encodeURL2");
            assertEquals("/encodeURL;jsessionid=" + sessionId, encodeURL2);

            String encodeURL3 = props.get("encodeURL3");
            assertEquals("http://localhost:" + PORT + "/;jsessionid=" + sessionId, encodeURL3);

            String encodeRedirectURL1 = props.get("encodeRedirectURL1");
            assertEquals("/encodeRedirectURL", encodeRedirectURL1);

            String encodeRedirectURL2 = props.get("encodeRedirectURL2");
            assertEquals("/encodeRedirectURL;jsessionid=" + sessionId, encodeRedirectURL2);

            String encodeRedirectURL3 = props.get("encodeRedirectURL3");
            assertEquals("", encodeRedirectURL3);
        } finally {
            server.shutdownNow();
        }
    }

    @SuppressWarnings({ "unchecked" })
    private HttpPacket createRequest(String uri, Map<String, String> headers) {

        HttpRequestPacket.Builder b = HttpRequestPacket.builder();
        b.method(Method.GET).protocol(Protocol.HTTP_1_1).uri(uri).header("Host", "localhost:" + PORT);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                b.header(entry.getKey(), entry.getValue());
            }
        }

        return b.build();
    }

    private HttpContent doTest(HttpHandler httpHandler, HttpPacket request, int timeout) throws Exception {

        HttpServer server = createWebServer(httpHandler);
        try {
            Thread.sleep(10);
            server.start();
            return sendRequest(request, timeout);
        } finally {
            server.shutdownNow();
        }
    }

    private HttpServer createWebServer(HttpHandler httpHandler) {

        HttpServer server = new HttpServer();
        NetworkListener listener = new NetworkListener("grizzly", NetworkListener.DEFAULT_NETWORK_HOST, PORT);
        listener.getKeepAlive().setIdleTimeoutInSeconds(-1);
        server.addListener(listener);
        server.getServerConfiguration().addHttpHandler(httpHandler, "/");

        return server;

    }

    private HttpContent sendRequest(HttpPacket request, int timeout) throws Exception {

        HttpConnection connection = connect();

        try {
            return connection.send(request).get(timeout, TimeUnit.SECONDS);
        } finally {
            connection.close();
        }
    }

    private HttpConnection connect() {
        TCPNIOTransport clientTransport = TCPNIOTransportBuilder.newInstance().build();

        FilterChainBuilder clientFilterChainBuilder = FilterChainBuilder.stateless();
        clientFilterChainBuilder.add(new TransportFilter());
        clientFilterChainBuilder.add(new ChunkingFilter(5));
        clientFilterChainBuilder.add(new HttpClientFilter());

        ClientFilter clientFilter = new ClientFilter();

        clientFilterChainBuilder.add(clientFilter);
        clientTransport.setProcessor(clientFilterChainBuilder.build());

        Connection connection = null;

        try {
            clientTransport.start();
            Future<Connection> connectFuture = clientTransport.connect("localhost", PORT);
            connection = connectFuture.get(10, TimeUnit.SECONDS);

            return new HttpConnection(clientTransport, connection, clientFilter);
        } catch (Exception e) {
            if (connection != null) {
                connection.closeSilently();
            }

            try {
                clientTransport.shutdownNow();
            } catch (IOException ee) {
            }

            throw new IllegalStateException(e);
        }
    }

    private Cookie[] getCookies(MimeHeaders headers) {
        Cookies cookies = new Cookies();
        cookies.setHeaders(headers, false);
        return cookies.get();
    }

    private String[] getHeaderValues(MimeHeaders headers, String name) {
        List<String> values = new ArrayList<>();

        for (String value : headers.values(name)) {
            values.add(value);
        }

        return values.toArray(new String[values.size()]);
    }

    private static class HttpConnection {
        private Transport transport;
        private Connection connection;
        private ClientFilter clientFilter;

        private HttpConnection(TCPNIOTransport transport, Connection connection, ClientFilter clientFilter) {
            this.transport = transport;
            this.connection = connection;
            this.clientFilter = clientFilter;
        }

        public void close() throws IOException {
            connection.close();
            transport.shutdownNow();
        }

        private Future<HttpContent> send(HttpPacket request) {
            clientFilter.reset();
            connection.write(request);
            return clientFilter.testFuture;
        }
    }

    private static class ClientFilter extends BaseFilter {
        private static Logger logger = Grizzly.logger(ClientFilter.class);

        private FutureImpl<HttpContent> testFuture;

        // -------------------------------------------------------- Constructors

        public ClientFilter() {
        }

        public void reset() {
            testFuture = SafeFutureImpl.create();
        }

        // ------------------------------------------------- Methods from Filter

        @Override
        public NextAction handleRead(FilterChainContext ctx) throws IOException {

            // Cast message to a HttpContent
            HttpContent httpContent = ctx.getMessage();

            logger.log(Level.FINE, "Got HTTP response chunk");

            // Get HttpContent's Buffer
            Buffer buffer = httpContent.getContent();

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "HTTP content size: {0}", buffer.remaining());
            }

            if (!httpContent.isLast()) {
                return ctx.getStopAction(httpContent);
            }

            testFuture.result(httpContent);

            return ctx.getStopAction();
        }

        @Override
        public NextAction handleClose(FilterChainContext ctx) throws IOException {
            close();
            return ctx.getStopAction();
        }

        private void close() throws IOException {

            if (!testFuture.isDone()) {
                // noinspection ThrowableInstanceNeverThrown
                testFuture.failure(new IOException("Connection was closed"));
            }

        }

    } // END ClientFilter

    public static class HttpSessionHandler extends HttpHandler {

        @Override
        public void service(Request request, Response response) throws Exception {
            String sessionId = request.getRequestedSessionId();
            if (sessionId != null) {
                response.getWriter().write("session-id=" + sessionId + "\n");
            }

            String jrouteId = request.getJrouteId();
            if (jrouteId != null) {
                response.getWriter().write("jroute-id=" + jrouteId + "\n");
            }
        }

    }

    public static class HttpCreaeteSessionHandler extends HttpHandler {

        @Override
        public void service(Request request, Response response) throws Exception {
            Session session = request.getSession(true);
            if (session != null) {
                response.getWriter().write("session-id=" + session.getIdInternal() + "\n");
            } else {
                response.getWriter().write("FAILED\n");
            }
        }
    }

    public static class HttpEncodeURLHandler extends HttpHandler {

        @Override
        public void service(Request request, Response response) throws Exception {
            String encodeURL1 = response.encodeURL("/encodeURL");
            String encodeRedirectURL1 = response.encodeRedirectURL("/encodeRedirectURL");

            Session session = request.getSession();

            String encodeURL2 = response.encodeURL("/encodeURL");
            String encodeRedirectURL2 = response.encodeRedirectURL("/encodeRedirectURL");

            String encodeURL3 = response.encodeURL("");
            String encodeRedirectURL3 = response.encodeRedirectURL("");

            if (session != null) {
                response.getWriter().write("session-id=" + session.getIdInternal() + "\n");
                response.getWriter().write("encodeURL1=" + encodeURL1 + "\n");
                response.getWriter().write("encodeRedirectURL1=" + encodeRedirectURL1 + "\n");
                response.getWriter().write("encodeURL2=" + encodeURL2 + "\n");
                response.getWriter().write("encodeRedirectURL2=" + encodeRedirectURL2 + "\n");
                response.getWriter().write("encodeURL3=" + encodeURL3 + "\n");
                response.getWriter().write("encodeRedirectURL3=" + encodeRedirectURL3 + "\n");
            } else {
                response.getWriter().write("FAILED\n");
            }
        }
    }
}
