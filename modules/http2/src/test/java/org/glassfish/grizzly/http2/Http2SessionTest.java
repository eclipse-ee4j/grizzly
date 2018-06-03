package org.glassfish.grizzly.http2;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.DefaultFilterChain;
import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http2.frames.ErrorCode;
import org.glassfish.grizzly.memory.ByteBufferManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test {@link Http2Session}
 * @author sdiedrichsen
 * @version $Id$
 * @since 26.05.18
 */
public class Http2SessionTest {

    @Mock
    private Connection<?> connection;
    @Mock
    private Http2Configuration configuration;
    @Mock
    private Filter filter;

    private Http2Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        MemoryManager memoryManager = new ByteBufferManager();
        FilterChain filterChain = new DefaultFilterChain();
        filterChain.add(filter);
        doReturn(filterChain).when(connection).getProcessor();
        doReturn(memoryManager).when(connection).getMemoryManager();
        doReturn(0.01f).when(configuration).getStreamsHighWaterMark();
        doReturn(1.0f).when(configuration).getCleanPercentage();
        Http2BaseFilter handlerFilter = new Http2ServerFilter(configuration);
        session = new Http2Session(connection, true, handlerFilter);
        FilterChainContext context = FilterChainContext.create(connection);
        context.getInternalContext().setProcessor(filterChain);
        context.setFilterIdx(0);
        context.setEndIdx(-1);
        session.setupFilterChains(context, false);
    }

    @Test
    public void testTerminateClosedStreams() {
        Http2Stream stream = mock(Http2Stream.class);
        doReturn(true).when(stream).isClosed();
        session.registerStream(1, stream);
        session.registerStream(2, stream);
        session.registerStream(3, stream);
        session.terminate(ErrorCode.INTERNAL_ERROR, "Test");
    }
}