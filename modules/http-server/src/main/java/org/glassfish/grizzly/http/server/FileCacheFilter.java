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

package org.glassfish.grizzly.http.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.FileTransfer;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.OutputSink;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpContext;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.filecache.FileCache;
import org.glassfish.grizzly.http.server.filecache.FileCache.CacheType;
import org.glassfish.grizzly.http.server.filecache.FileCacheEntry;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.memory.Buffers;

/**
 *
 * @author oleksiys
 */
public class FileCacheFilter extends BaseFilter {
    private static final Logger LOGGER = Grizzly.logger(FileCacheFilter.class);
    
    private final FileCache fileCache;

    public FileCacheFilter(FileCache fileCache) {

        this.fileCache = fileCache;

    }


    // ----------------------------------------------------- Methods from Filter


    @Override
    public NextAction handleRead(final FilterChainContext ctx) throws IOException {

        final HttpContent requestContent = ctx.getMessage();
        final HttpRequestPacket request = (HttpRequestPacket) requestContent.getHttpHeader();

        if (fileCache.isEnabled() && Method.GET.equals(request.getMethod())) {
            final FileCacheEntry cacheEntry = fileCache.get(request);
            if (cacheEntry != null) {
                final HttpResponsePacket response = request.getResponse();
                prepareResponse(cacheEntry, response);
                
                if (response.getStatus() != 200) {
                    // The cache hit - return empty response
                    ctx.write(HttpContent.builder(response)
                            .content(Buffers.EMPTY_BUFFER)
                            .last(true)
                            .build());
                    
                    return flush(ctx);
                }

                // check if we can send plain or compressed data back.
                // depends on client request headers and file cache entry
                final boolean isServeCompressed =
                        cacheEntry.canServeCompressed(request);
                
                // The client doesn't have this resource cached, so
                // we have to send entire payload
                prepareResponseWithPayload(cacheEntry, response,
                        isServeCompressed);

                if (cacheEntry.type != CacheType.FILE) {
                    // the payload is available in a ByteBuffer
                    final Buffer buffer = Buffers.wrap(ctx.getMemoryManager(),
                            cacheEntry.getByteBuffer(isServeCompressed).duplicate());

                    ctx.write(HttpContent.builder(response)
                            .content(buffer)
                            .last(true)
                            .build());

                    return flush(ctx);
                }
                
                return fileCache.isFileSendEnabled() && !request.isSecure()
                        ? sendFileZeroCopy(ctx, response, cacheEntry,
                            isServeCompressed)
                        : sendFileUsingBuffers(ctx, response, cacheEntry,
                            isServeCompressed);
            }
        }

        return ctx.getInvokeAction();

    }

    public FileCache getFileCache() {
        return fileCache;
    }
    
    /**
     * Prepares common response headers.
     */
    private void prepareResponse(final FileCacheEntry entry,
            final HttpResponsePacket response) throws IOException {
        response.setContentType(entry.contentType.prepare());

        if (entry.server != null) {
            response.addHeader(Header.Server, entry.server);
        }
    }
    
    
    /**
     * Prepare response with payload headers.
     */
    private void prepareResponseWithPayload(final FileCacheEntry entry,
            final HttpResponsePacket response, final boolean isServeCompressed)
            throws IOException {
        response.addHeader(Header.ETag, entry.Etag);
        response.addHeader(Header.LastModified, entry.lastModifiedHeader);

        response.setContentLengthLong(entry.getFileSize(isServeCompressed));
        
        if (isServeCompressed) {
            response.addHeader(Header.ContentEncoding, "gzip");
        }
    }

    private NextAction sendFileUsingBuffers(final FilterChainContext ctx,
            final HttpResponsePacket response, final FileCacheEntry cacheEntry,
            final boolean isServeCompressed) {
        try {
            final FileSendEntry sendEntry = FileSendEntry.create(ctx, response,
                    cacheEntry.getFile(isServeCompressed),
                    cacheEntry.getFileSize(isServeCompressed));
            
            ctx.suspend();
            sendEntry.send();
            return ctx.getSuspendAction();
        } catch (IOException e) {
        }

        // FAILURE
        return ctx.getInvokeAction();
    }
    
    private NextAction sendFileZeroCopy(final FilterChainContext ctx,
            final HttpResponsePacket response, final FileCacheEntry cacheEntry,
            final boolean isServeCompressed) {
        
        // flush response
        ctx.write(response);

        // send-file
        final FileTransfer f = new FileTransfer(
                cacheEntry.getFile(isServeCompressed),
                0, cacheEntry.getFileSize(isServeCompressed));
        ctx.write(f, new EmptyCompletionHandler<WriteResult>() {
            @Override
            public void failed(Throwable throwable) {
                LOGGER.log(Level.FINE, "Error reported during file-send entry: " +
                        cacheEntry, throwable);
            }
        });

        
        return flush(ctx);
    }
    
    private NextAction flush(final FilterChainContext ctx) {
        final HttpContext httpContext = HttpContext.get(ctx);
        assert httpContext != null;
        final OutputSink output = httpContext.getOutputSink();

        if (output.canWrite()) {  // if connection write queue is not overloaded
            return ctx.getStopAction();
        } else { // if connection write queue is overloaded

            // prepare context for suspend
            final NextAction suspendAction = ctx.getSuspendAction();
            ctx.suspend();

            // notify when connection becomes writable, so we can resume it
            output.notifyCanWrite(new WriteHandler() {
                @Override
                public void onWritePossible() throws Exception {
                    finish();
                }

                @Override
                public void onError(Throwable t) {
                    finish();
                }

                private void finish() {
                    ctx.completeAndRecycle();
                }
            });

            return suspendAction;
        }
    }
    
    private static class FileSendEntry implements WriteHandler {
        private final FilterChainContext ctx;
        private final FileChannel fc;
        private final FileInputStream fis;
        private final HttpResponsePacket response;
        private final OutputSink output;
        
        private long remaining;

        public static FileSendEntry create(final FilterChainContext ctx,
                final HttpResponsePacket response,
                final File file, final long size) throws IOException {
            
            final FileInputStream fis = new FileInputStream(file);
            final FileChannel fc = fis.getChannel();
            
            return new FileSendEntry(ctx, response, fis, fc, size);
        }
        
        public FileSendEntry(final FilterChainContext ctx,
                final HttpResponsePacket response,
                final FileInputStream fis, final FileChannel fc,
                final long size) {

            this.ctx = ctx;
            this.response = response;
            this.fis = fis;
            this.fc = fc;
            this.remaining = size;
            
            final HttpContext httpContext = response.getProcessingState().getHttpContext();
            assert httpContext != null;
            output = httpContext.getOutputSink();
        }
        
        
        public void close() {
            try {
                fis.close();
            } catch (IOException ignored) {
            }
        }

        private void send() {
            final int chunkSize = 8192;

            try {
                boolean isLast;
                do {
                    final Buffer buffer = ctx.getMemoryManager().allocate(chunkSize);
                    buffer.allowBufferDispose(true);
                    
                    final long readNow = Buffers.readFromFileChannel(fc, buffer);
                    isLast = readNow <= 0 || (remaining -= readNow) <= 0;

                    buffer.trim();
                    ctx.write(HttpContent.builder(response)
                            .content(buffer)
                            .last(isLast)
                            .build());
                    
                } while (!isLast && output.canWrite());
                
                if (isLast) {
                    done();
                } else {
                    output.notifyCanWrite(this);
                }
            } catch (IOException e) {
                done();
            }
        }

        private void done() {
            close();
            ctx.resume(ctx.getStopAction());
        }

        // --------------- WriteHandler ------------------------
        @Override
        public void onWritePossible() throws Exception {
            send();
        }

        @Override
        public void onError(Throwable t) {
            done();
        }
    }
}
