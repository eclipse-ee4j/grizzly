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

import static org.mockito.Mockito.*;

/**
 * Test {@link Http2Session}
 * @author Sven Diedrichsen (sven.diedrichsen@gmail.com)
 * @since 26.05.18
 */
public class Http2SessionTest {

    private Http2Configuration configuration =
            Http2Configuration.builder()
                .cleanPercentage(1.0f)
                .streamsHighWaterMark(0.01f).build();

    private Http2Session session;

    @Before
    public void setUp() {
        FilterChain filterChain = newFilterChain();
        Connection<?> connection = newConnectionMock(filterChain);
        session = new Http2Session(
            connection,
            true,
            new Http2ServerFilter(configuration)
        );
        session.setupFilterChains(
            newFilterChainContext(filterChain, connection),
            false
        );
    }

    private FilterChainContext newFilterChainContext(FilterChain filterChain, Connection<?> connection) {
        FilterChainContext context = FilterChainContext.create(connection);
        context.getInternalContext().setProcessor(filterChain);
        context.setFilterIdx(0);
        context.setEndIdx(-1);
        return context;
    }

    private FilterChain newFilterChain() {
        FilterChain filterChain = new DefaultFilterChain();
        filterChain.add(mock(Filter.class));
        return filterChain;
    }

    private Connection<?> newConnectionMock(FilterChain filterChain) {
        Connection<?> connection = mock(Connection.class);
        MemoryManager memoryManager = new ByteBufferManager();
        doReturn(filterChain).when(connection).getProcessor();
        doReturn(memoryManager).when(connection).getMemoryManager();
        return connection;
    }

    @Test
    public void testTerminateClosedStreams() {
        Http2Stream stream = mock(Http2Stream.class);
        doReturn(true).when(stream).isClosed();
        session.registerStream(1, stream);
        session.registerStream(2, stream);
        session.registerStream(3, stream);

        session.terminate(ErrorCode.INTERNAL_ERROR, "Test");

        verify(stream, times(3)).closedRemotely();
    }

}
