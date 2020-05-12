package org.glassfish.grizzly.servlet;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.junit.Test;

import jakarta.servlet.*;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests {@link WebappContext}.
 */
public class WebappContextTest {

    @Test
    public void testServletInitializationHappenOnlyOnce() {
        WebappContext context = new WebappContext();
        ServletRegistration servletRegistration = context.addServlet("TestServlet", TestServlet.class);
        servletRegistration.setLoadOnStartup(1);
        HttpServer server = new HttpServer();

        context.deploy(server);

        ServerConfiguration serverConfiguration = server.getServerConfiguration();
        Map<HttpHandler, HttpHandlerRegistration[]> httpHandlersWithMapping = serverConfiguration.getHttpHandlersWithMapping();
        ServletHandler servletHandler = (ServletHandler) httpHandlersWithMapping.entrySet().iterator().next().getKey();
        TestServlet servlet = (TestServlet) servletHandler.getServletInstance();

        assertThat("Unexpected number of servlet instances created.", TestServlet.instancesCreated(), is(1));
        assertThat("Unexpected number of servlet mappings found", httpHandlersWithMapping.size(), is(1));
        assertThat("Unexpected number of init calls on servlet.", servlet.initCalled(), is(1));
    }

    public static class TestServlet implements Servlet {

        private static int CREATION_COUNTER = 0;

        private int initializationCounter = 0;

        public TestServlet() {
            CREATION_COUNTER++;
        }

        public static int instancesCreated() {
            return CREATION_COUNTER;
        }

        public int initCalled() {
            return initializationCounter;
        }

        @Override
        public void init(ServletConfig servletConfig) throws ServletException {
            initializationCounter++;
        }

        @Override
        public ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {

        }
    }
}
