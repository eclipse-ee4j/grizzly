/*
 * Copyright (c) 2012, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.http2;

import static org.glassfish.grizzly.http2.Termination.OUT_FIN_TERMINATION;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.asyncqueue.AsyncQueueRecord;
import org.glassfish.grizzly.asyncqueue.MessageCloner;
import org.glassfish.grizzly.asyncqueue.TaskQueue;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.HttpTrailer;
import org.glassfish.grizzly.http2.frames.ErrorCode;
import org.glassfish.grizzly.http2.frames.HeadersFrame;
import org.glassfish.grizzly.http2.frames.Http2Frame;
import org.glassfish.grizzly.http2.frames.PushPromiseFrame;
import org.glassfish.grizzly.http2.utils.ChunkedCompletionHandler;
import org.glassfish.grizzly.memory.Buffers;

/**
 * Default implementation of an output sink, which is associated with specific {@link Http2Stream}.
 *
 * The implementation is aligned with HTTP/2 requirements with regards to message flow control.
 *
 * @author Alexey Stashok
 */
class DefaultOutputSink implements StreamOutputSink {
    private static final Logger LOGGER = Grizzly.logger(StreamOutputSink.class);

    private static final int MAX_OUTPUT_QUEUE_SIZE = 65536;

    private static final int ZERO_QUEUE_RECORD_SIZE = 1;

    private static final OutputQueueRecord TERMINATING_QUEUE_RECORD = new OutputQueueRecord(null, null, true, true);

    // async output queue
    final TaskQueue<OutputQueueRecord> outputQueue = TaskQueue.createTaskQueue(new TaskQueue.MutableMaxQueueSize() {

        @Override
        public int getMaxQueueSize() {
            return MAX_OUTPUT_QUEUE_SIZE;
        }
    });

    // the space (in bytes) in flow control window, that still could be used.
    // in other words the number of bytes, which could be sent to the peer
    private final AtomicInteger availStreamWindowSize;

    // true, if last output frame has been queued
    private volatile boolean isLastFrameQueued;
    // not null if last output frame has been sent or forcibly terminated
    private Termination terminationFlag;

    // associated HTTP/2 session
    private final Http2Session http2Session;
    // associated HTTP/2 stream
    private final Http2Stream stream;

    // counter for unflushed writes
    private final AtomicInteger unflushedWritesCounter = new AtomicInteger();
    // sync object to count/notify flush handlers
    private final Object flushHandlersSync = new Object();
    // flush handlers queue
    private BundleQueue<CompletionHandler<Http2Stream>> flushHandlersQueue;

    DefaultOutputSink(final Http2Stream stream) {
        this.stream = stream;
        http2Session = stream.getHttp2Session();
        availStreamWindowSize = new AtomicInteger(stream.getPeerWindowSize());
    }

    @Override
    public boolean canWrite() {
        return outputQueue.size() < MAX_OUTPUT_QUEUE_SIZE;
    }

    @Override
    public void notifyWritePossible(final WriteHandler writeHandler) {
        outputQueue.notifyWritePossible(writeHandler, MAX_OUTPUT_QUEUE_SIZE);
    }

    private void assertReady() throws IOException {
        // if the last frame (fin flag == 1) has been queued already - throw an IOException
        if (isTerminated()) {
            if (LOGGER.isLoggable(FINE)) {
                LOGGER.log(FINE, "Terminated!!! id={0} description={1}", new Object[] { stream.getId(), terminationFlag.getDescription() });
            }
            throw new IOException(terminationFlag.getDescription());
        } else if (isLastFrameQueued) {
            throw new IOException("Write beyond end of stream");
        }
    }

    /**
     * The method is called by HTTP2 Filter once WINDOW_UPDATE message comes for this {@link Http2Stream}.
     *
     * @param delta the delta.
     * @throws Http2StreamException if an error occurs processing the window update.
     */
    @Override
    public void onPeerWindowUpdate(final int delta) throws Http2StreamException {

        final int currentWindow = availStreamWindowSize.get();
        if (delta > 0 && currentWindow > 0 && currentWindow + delta < 0) {
            throw new Http2StreamException(stream.getId(), ErrorCode.FLOW_CONTROL_ERROR, "Session flow-control window overflow.");
        }
        // update the available window size
        availStreamWindowSize.addAndGet(delta);

        // try to write until window limit allows
        while ((isWantToWrite() && !outputQueue.isEmpty())
            || (outputQueue.peek() != null && outputQueue.peek().trailer != null)) {

            // pick up the first output record in the queue
            OutputQueueRecord outputQueueRecord = outputQueue.poll();

            // if there is nothing to write - return
            if (outputQueueRecord == null) {
                return;
            }

            // if it's terminating record - processFin
            if (outputQueueRecord == TERMINATING_QUEUE_RECORD) {
                // if it's TERMINATING_QUEUE_RECORD - don't forget to release ATOMIC_QUEUE_RECORD_SIZE
                releaseWriteQueueSpace(0, true, true);
                writeEmptyFin();
                return;
            }

            final FlushCompletionHandler completionHandler = outputQueueRecord.chunkedCompletionHandler;
            boolean isLast = outputQueueRecord.isLast;
            final boolean isZeroSizeData = outputQueueRecord.isZeroSizeData;
            final Source resource = outputQueueRecord.resource;

            final HttpTrailer currentTrailer = outputQueueRecord.trailer;
            final MessageCloner<Buffer> messageCloner = outputQueueRecord.cloner;

            if (currentTrailer != null) {
                try {
                    outputQueueRecord = null;
                    sendTrailers(completionHandler, messageCloner, currentTrailer);
                } catch (IOException ex) {
                    LOGGER.log(WARNING, "Error sending trailers.", ex);
                }
                return;
            }

            // check if output record's buffer is fitting into window size
            // if not - split it into 2 parts: part to send, part to keep in the queue
            final int bytesToSend = checkOutputWindow(resource.remaining());
            final Buffer dataChunkToSend = resource.read(bytesToSend);
            final boolean hasRemaining = resource.hasRemaining();

            // if there is a chunk to store
            if (hasRemaining) {
                // Create output record for the chunk to be stored
                outputQueueRecord.reset(resource, completionHandler, isLast);
                outputQueueRecord.incChunksCounter();

                // reset isLast for the current chunk
                isLast = false;
            } else {
                outputQueueRecord.release();
                outputQueueRecord = null;
            }

            // if there is a chunk to sent
            if (dataChunkToSend != null && (dataChunkToSend.hasRemaining() || isLast)) {
                final int dataChunkToSendSize = dataChunkToSend.remaining();

                // send a http2 data frame
                flushToConnectionOutputSink(dataChunkToSend, completionHandler, isLast);

                // update the available window size bytes counter
                availStreamWindowSize.addAndGet(-dataChunkToSendSize);
                releaseWriteQueueSpace(dataChunkToSendSize, isZeroSizeData, outputQueueRecord == null);

                outputQueue.doNotify();
            } else if (isZeroSizeData && outputQueueRecord == null) {
                // if it's atomic and no remainder left - don't forget to release ATOMIC_QUEUE_RECORD_SIZE
                releaseWriteQueueSpace(0, true, true);
                outputQueue.doNotify();
            }

            if (outputQueueRecord != null) {
                // if there is a chunk to be stored - store it and return
                outputQueue.setCurrentElement(outputQueueRecord);
                break;
            }
        }
    }

    /**
     * Send an {@link HttpPacket} to the {@link Http2Stream}.
     *
     * The writeDownStream(...) methods have to be synchronized with shutdown().
     *
     * @param httpPacket {@link HttpPacket} to send
     * @param completionHandler the {@link CompletionHandler}, which will be notified about write progress.
     * @param messageCloner the {@link MessageCloner}, which will be able to clone the message in case it can't be
     * completely written in the current thread.
     * @throws IOException if an error occurs with the write operation.
     */
    @Override
    public synchronized void writeDownStream(final HttpPacket httpPacket, final FilterChainContext ctx,
            final CompletionHandler<WriteResult> completionHandler, final MessageCloner<Buffer> messageCloner)
                throws IOException {
        assert ctx != null;

        assertReady();

        final HttpHeader httpHeader = stream.getOutputHttpHeader();
        final HttpContent httpContent = HttpContent.isContent(httpPacket) ? (HttpContent) httpPacket : null;

        List<Http2Frame> headerFrames = null;
        OutputQueueRecord outputQueueRecord = null;

        try { // try-finally block to release deflater lock if needed

            // If HTTP header hasn't been committed - commit it
            if (!httpHeader.isCommitted()) {
                // do we expect any HTTP payload?
                final boolean isNoPayload = !httpHeader.isExpectContent()
                    || (httpContent != null && httpContent.isLast() && !httpContent.getContent().hasRemaining());

                http2Session.getDeflaterLock().lock();
                final boolean logging = NetLogger.isActive();
                final Map<String, String> capture = logging ? new HashMap<>() : null;
                headerFrames = http2Session.encodeHttpHeaderAsHeaderFrames(ctx, httpHeader, stream.getId(), isNoPayload, null, capture);
                if (logging) {
                    for (Http2Frame http2Frame : headerFrames) {
                        if (http2Frame.getType() == PushPromiseFrame.TYPE) {
                            NetLogger.log(NetLogger.Context.TX, http2Session, (HeadersFrame) http2Frame, capture);
                            break;
                        }
                    }
                }
                stream.onSndHeaders(isNoPayload);

                // 100-Continue block
                if (!httpHeader.isRequest()) {
                    HttpResponsePacket response = (HttpResponsePacket) httpHeader;
                    if (response.isAcknowledgement()) {
                        response.acknowledged();
                        response.getHeaders().clear();
                        unflushedWritesCounter.incrementAndGet();
                        flushToConnectionOutputSink(headerFrames, completionHandler, messageCloner, false);
                        return;
                    }
                }

                httpHeader.setCommitted(true);

                if (isNoPayload || httpContent == null) {
                    // if we don't expect any HTTP payload, mark this frame as
                    // last and return
                    unflushedWritesCounter.incrementAndGet();
                    flushToConnectionOutputSink(headerFrames, completionHandler, messageCloner, isNoPayload);
                    return;
                }
            }

            // if there is nothing to write - return
            if (httpContent == null) {
                return;
            }

            http2Session.handlerFilter.onHttpContentEncoded(httpContent, ctx);

            boolean isLast = httpContent.isLast();
            final boolean isTrailer = HttpTrailer.isTrailer(httpContent);
            Buffer data = httpContent.getContent();
            final int dataSize = data.remaining();

            unflushedWritesCounter.incrementAndGet();
            final FlushCompletionHandler flushCompletionHandler = new FlushCompletionHandler(completionHandler);

            boolean isDataCloned = false;

            final boolean isZeroSizeData = dataSize == 0;
            final int spaceToReserve = isZeroSizeData ? ZERO_QUEUE_RECORD_SIZE : dataSize;

            // Check if output queue is not empty - add new element
            if (reserveWriteQueueSpace(spaceToReserve) > spaceToReserve) {
                // if the queue is not empty - the headers should have been sent
                assert headerFrames == null;

                if (messageCloner != null) {
                    data = messageCloner.clone(http2Session.getConnection(), data);
                    isDataCloned = true;
                }

                if (isTrailer) {
                    outputQueueRecord = new OutputQueueRecord(
                            Source.factory(stream).createBufferSource(data),
                            flushCompletionHandler,
                            (HttpTrailer) httpContent,
                            false);
                } else {
                    outputQueueRecord = new OutputQueueRecord(
                            Source.factory(stream).createBufferSource(data),
                            flushCompletionHandler, isLast, isZeroSizeData);
                }

                outputQueue.offer(outputQueueRecord);

                // check if our element wasn't forgotten (async)
                if (outputQueue.size() != spaceToReserve || !outputQueue.remove(outputQueueRecord)) {
                    // if not - send trailers and return
                    if (isLast && isTrailer) {
                        sendTrailers(completionHandler, messageCloner, (HttpTrailer) httpContent);
                    }
                    return;
                }

                outputQueueRecord = null;
            }

            // our element is first in the output queue
            final int remaining = data.remaining();

            // check if output record's buffer is fitting into window size
            // if not - split it into 2 parts: part to send, part to keep in the queue
            final int fitWindowLen = checkOutputWindow(remaining);

            // if there is a chunk to store
            if (fitWindowLen < remaining) {
                if (!isDataCloned && messageCloner != null) {
                    data = messageCloner.clone(http2Session.getConnection(), data);
                    isDataCloned = true;
                }

                final Buffer dataChunkToStore = splitOutputBufferIfNeeded(data, fitWindowLen);

                // Create output record for the chunk to be stored
                outputQueueRecord = new OutputQueueRecord(
                        Source.factory(stream).createBufferSource(dataChunkToStore),
                        flushCompletionHandler, isLast, isZeroSizeData);

                // reset completion handler and isLast for the current chunk
                isLast = false;
            }

            // if there's anything to send - send it
            final Buffer dataToSend = prepareDataToSend(outputQueueRecord == null, isLast, data, isZeroSizeData);
            if (headerFrames != null || dataToSend != null) {
                // if another part of data is stored in the queue -
                // we have to increase CompletionHandler counter to avoid
                // premature notification
                if (outputQueueRecord != null) {
                    outputQueueRecord.incChunksCounter();
                }

                flushToConnectionOutputSink(headerFrames, dataToSend, flushCompletionHandler,
                        isDataCloned ? null : messageCloner, isLast && !isTrailer);
            }

            if (isLast) {
                if (isTrailer) {
                    sendTrailers(completionHandler, messageCloner, (HttpTrailer) httpContent);
                }
                return;
            }

        } finally {
            // if it is locked by this thread, it was locked in the first lines of this try block
            if (http2Session.getDeflaterLock().isHeldByCurrentThread()) {
                http2Session.getDeflaterLock().unlock();
            }
        }

        if (outputQueueRecord == null) {
            return;
        }
        addOutputQueueRecord(outputQueueRecord);
    }


    private Buffer prepareDataToSend(final boolean isRecordNull, final boolean isLast, final Buffer data,
        final boolean isZeroSizeData) {
        if (data == null) {
            return null;
        }
        if (data.hasRemaining() || isLast) {
            final int dataChunkToSendSize = data.remaining();
            // update the available window size bytes counter
            availStreamWindowSize.addAndGet(-dataChunkToSendSize);
            releaseWriteQueueSpace(dataChunkToSendSize, isZeroSizeData, isRecordNull);
            return data;
        }
        return null;
    }



    /**
     * Flush {@link Http2Stream} output and notify {@link CompletionHandler} once all output data has been flushed.
     *
     * @param completionHandler {@link CompletionHandler} to be notified
     */
    @Override
    public void flush(final CompletionHandler<Http2Stream> completionHandler) {

        // check if there are pending unflushed data
        if (unflushedWritesCounter.get() > 0) {
            // if yes - synchronize do disallow decrease counter from other thread (increasing is ok)
            synchronized (flushHandlersSync) {
                // double check the pending flushes counter
                final int counterNow = unflushedWritesCounter.get();
                if (counterNow > 0) {
                    // if there are pending flushes
                    if (flushHandlersQueue == null) {
                        // create a flush handlers queue
                        flushHandlersQueue = new BundleQueue<>();
                    }

                    // add the handler to the queue
                    flushHandlersQueue.add(counterNow, completionHandler);

                    return;
                }
            }
        }

        // if there are no pending flushes - notify the handler
        completionHandler.completed(stream);
    }

    /**
     * The method is responsible for checking the current output window size.
     * The returned integer value is the size of the data, which could be
     * sent now.
     *
     * @param size check the provided size against the window size limit.
     *
     * @return the amount of data that may be written.
     */
    private int checkOutputWindow(final long size) {
        // take a snapshot of the current output window state and check if we
        // can fit "size" into window.
        // Make sure we return positive value or zero, because availStreamWindowSize could be negative.
        return Math.max(0, Math.min(availStreamWindowSize.get(), (int) size));
    }

    private Buffer splitOutputBufferIfNeeded(final Buffer buffer, final int length) {
        if (length == buffer.remaining()) {
            return null;
        }

        return buffer.split(buffer.position() + length);
    }

    private void flushToConnectionOutputSink(final List<Http2Frame> headerFrames,
        final CompletionHandler<WriteResult> completionHandler,
        final MessageCloner<Buffer> messageCloner,
        final boolean isLast) {
        final FlushCompletionHandler flushCompletionHandler = new FlushCompletionHandler(completionHandler);
        flushToConnectionOutputSink(headerFrames, null, flushCompletionHandler, messageCloner, isLast);
    }

    private void flushToConnectionOutputSink(
        final Buffer data,
        final FlushCompletionHandler flushCompletionHandler,
        final boolean isLast) {
        flushToConnectionOutputSink(null, data, flushCompletionHandler, null, isLast);
    }

    private void flushToConnectionOutputSink(
            final List<Http2Frame> headerFrames,
            final Buffer data,
            final CompletionHandler<WriteResult> completionHandler,
            final MessageCloner<Buffer> messageCloner,
            final boolean isLast) {

        http2Session.getOutputSink().writeDataDownStream(
            stream, headerFrames, data, completionHandler, messageCloner, isLast);

        if (isLast) {
            terminate(OUT_FIN_TERMINATION);
        }
    }

    /**
     * Closes the output sink by adding last DataFrame with the FIN flag to a queue. If the output sink is already closed -
     * method does nothing.
     */
    @Override
    public synchronized void close() {
        if (!isClosed()) {
            isLastFrameQueued = true;

            if (outputQueue.isEmpty()) {
                writeEmptyFin();
                return;
            }

            outputQueue.reserveSpace(ZERO_QUEUE_RECORD_SIZE);
            outputQueue.offer(TERMINATING_QUEUE_RECORD);

            if (outputQueue.size() == ZERO_QUEUE_RECORD_SIZE && outputQueue.remove(TERMINATING_QUEUE_RECORD)) {
                writeEmptyFin();
            }
        }
    }

    /**
     * Unlike {@link #close()} this method forces the output sink termination by setting termination flag and canceling all
     * the pending writes.
     */
    @Override
    public synchronized void terminate(final Termination terminationFlag) {
        if (!isTerminated()) {
            this.terminationFlag = terminationFlag;
            outputQueue.onClose();
            // NOTIFY STREAM
            stream.onOutputClosed();
        }
    }

    @Override
    public boolean isClosed() {
        return isLastFrameQueued || isTerminated();
    }

    /**
     * @return the number of writes (not bytes), that haven't reached network layer
     */
    @Override
    public int getUnflushedWritesCount() {
        return unflushedWritesCounter.get();
    }

    private boolean isTerminated() {
        return terminationFlag != null;
    }

    private void writeEmptyFin() {
        if (!isTerminated()) {
            unflushedWritesCounter.incrementAndGet();
            flushToConnectionOutputSink(Buffers.EMPTY_BUFFER, new FlushCompletionHandler(null), true);
        }
    }

    private boolean isWantToWrite() {
        // update the available window size
        final int availableWindowSizeBytesNow = availStreamWindowSize.get();

        // get the current peer's window size limit
        final int windowSizeLimit = stream.getPeerWindowSize();

        return availableWindowSizeBytesNow >= windowSizeLimit / 4;
    }

    private void addOutputQueueRecord(OutputQueueRecord outputQueueRecord) throws Http2StreamException {
        do {
            outputQueue.setCurrentElement(outputQueueRecord);

            // check if situation hasn't changed and we can't send the data chunk now
            if (!isWantToWrite() || !outputQueue.compareAndSetCurrentElement(outputQueueRecord, null)) {
                break; // will be (or already) written asynchronously
            }

            // if we can send the output record now - do that
            final FlushCompletionHandler chunkedCompletionHandler = outputQueueRecord.chunkedCompletionHandler;
            final HttpTrailer currentTrailer = outputQueueRecord.trailer;
            final MessageCloner<Buffer> messageCloner = outputQueueRecord.cloner;
            if (currentTrailer != null) {
                try {
                    sendTrailers(chunkedCompletionHandler, messageCloner, currentTrailer);
                } catch (IOException ex) {
                    LOGGER.log(WARNING, "Error sending trailers.", ex);
                }
                return;
            }

            boolean isLast = outputQueueRecord.isLast;
            final boolean isZeroSizeData = outputQueueRecord.isZeroSizeData;
            final Source currentResource = outputQueueRecord.resource;

            final int fitWindowLen = checkOutputWindow(currentResource.remaining());
            final Buffer dataChunkToSend = currentResource.read(fitWindowLen);

            // if there is a chunk to store
            if (currentResource.hasRemaining()) {
                // Create output record for the chunk to be stored
                outputQueueRecord.reset(currentResource, chunkedCompletionHandler, isLast);
                outputQueueRecord.incChunksCounter();

                // reset isLast for the current chunk
                isLast = false;
            } else {
                outputQueueRecord.release();
                outputQueueRecord = null;
            }

            // if there is a chunk to send
            if (dataChunkToSend != null && (dataChunkToSend.hasRemaining() || isLast)) {
                final int dataChunkToSendSize = dataChunkToSend.remaining();
                flushToConnectionOutputSink(dataChunkToSend, chunkedCompletionHandler, isLast);

                // update the available window size bytes counter
                availStreamWindowSize.addAndGet(-dataChunkToSendSize);
                releaseWriteQueueSpace(dataChunkToSendSize, isZeroSizeData, outputQueueRecord == null);
            } else if (isZeroSizeData && outputQueueRecord == null) {
                // if it's atomic and no remainder left - don't forget to release ATOMIC_QUEUE_RECORD_SIZE
                releaseWriteQueueSpace(0, true, true);
            } else if (dataChunkToSend != null && !dataChunkToSend.hasRemaining()) {
                // current window won't allow the data to be sent.  Will be written once the
                // window changes.
                if (outputQueueRecord != null) {
                    reserveWriteQueueSpace(outputQueueRecord.resource.remaining());
                    outputQueue.offer(outputQueueRecord);
                }
                break;
            }
        } while (outputQueueRecord != null);
    }

    private int reserveWriteQueueSpace(final int spaceToReserve) {
        return outputQueue.reserveSpace(spaceToReserve);
    }

    private void releaseWriteQueueSpace(final int justSentBytes, final boolean isAtomic, final boolean isEndOfChunk) {
        if (isEndOfChunk) {
            outputQueue.releaseSpace(isAtomic ? ZERO_QUEUE_RECORD_SIZE : justSentBytes);
        } else if (!isAtomic) {
            outputQueue.releaseSpace(justSentBytes);
        }
    }

    private void sendTrailers(final CompletionHandler<WriteResult> completionHandler,
        final MessageCloner<Buffer> messageCloner, final HttpTrailer httpContent)
    throws IOException {
        http2Session.getDeflaterLock().lock();
        try {
            final boolean logging = NetLogger.isActive();
            final Map<String,String> capture = logging ? new HashMap<>() : null;
            final List<Http2Frame> trailerFrames =
                    http2Session.encodeTrailersAsHeaderFrames(stream.getId(),
                            new ArrayList<>(4),
                            httpContent.getHeaders(), capture);
            if (logging) {
                for (Http2Frame http2Frame : trailerFrames) {
                    if (http2Frame.getType() == PushPromiseFrame.TYPE) {
                        NetLogger.log(NetLogger.Context.TX, http2Session, (HeadersFrame) http2Frame, capture);
                        break;
                    }
                }
            }
            unflushedWritesCounter.incrementAndGet();
            flushToConnectionOutputSink(trailerFrames, completionHandler, messageCloner, true);
            close();
        } finally {
            http2Session.getDeflaterLock().unlock();
        }
    }

    private static class OutputQueueRecord extends AsyncQueueRecord<WriteResult> {
        private Source resource;
        private FlushCompletionHandler chunkedCompletionHandler;

        private HttpTrailer trailer;
        private MessageCloner<Buffer> cloner;

        private boolean isLast;

        private final boolean isZeroSizeData;

        public OutputQueueRecord(final Source resource, final FlushCompletionHandler completionHandler,
                final boolean isLast, final boolean isZeroSizeData) {
            super(null, null, null);

            this.resource = resource;
            this.chunkedCompletionHandler = completionHandler;
            this.isLast = isLast;
            this.isZeroSizeData = isZeroSizeData;
        }

        public OutputQueueRecord(final Source resource,
                final FlushCompletionHandler completionHandler,
                final HttpTrailer trailer, final boolean isZeroDataSize) {
            super(null, null, null);

            this.resource = resource;
            this.chunkedCompletionHandler = completionHandler;
            this.isLast = true;
            this.trailer = trailer;
            this.isZeroSizeData = isZeroDataSize;
        }

        private void incChunksCounter() {
            if (chunkedCompletionHandler != null) {
                chunkedCompletionHandler.incChunks();
            }
        }

        private void reset(final Source resource, final FlushCompletionHandler completionHandler, final boolean last) {
            this.resource = resource;
            this.chunkedCompletionHandler = completionHandler;
            this.isLast = last;
        }

        public void release() {
            if (resource != null) {
                resource.release();
                resource = null;
            }
        }

        @Override
        public void notifyFailure(final Throwable e) {
            final CompletionHandler chLocal = chunkedCompletionHandler;
            chunkedCompletionHandler = null;
            try {
                if (chLocal != null) {
                    chLocal.failed(e);
                }
            } finally {
                release();
            }
        }

        @Override
        public void recycle() {
        }

        @Override
        public WriteResult getCurrentResult() {
            return null;
        }
    }

    /**
     * Flush {@link CompletionHandler}, which will be passed on each {@link Http2Stream} write to make sure
     * the data reached the wires.
     *
     * Usually <tt>FlushCompletionHandler</tt> is also used as a wrapper for custom {@link CompletionHandler}
     * provided by users.
     *
     * The parent class has an internal state, so be careful with reuses of the same instance!
     */
    private final class FlushCompletionHandler extends ChunkedCompletionHandler {

        public FlushCompletionHandler(final CompletionHandler<WriteResult> parentCompletionHandler) {
            super(parentCompletionHandler);
        }

        /**
         * Notifies all flush handlers about the completition.
         */
        @Override
        protected void done0() {
            synchronized (flushHandlersSync) { // synchronize with flush()
                unflushedWritesCounter.decrementAndGet();
                if (flushHandlersQueue == null || !flushHandlersQueue.nextBundle()) {
                    return;
                }
            }

            boolean hasNext;
            CompletionHandler<Http2Stream> handler;

            do {
                synchronized (flushHandlersSync) {
                    handler = flushHandlersQueue.next();
                    hasNext = flushHandlersQueue.hasNext();
                }

                try {
                    handler.completed(stream);
                } catch (Exception ignored) {
                }
            } while (hasNext);
        }
    }
}
