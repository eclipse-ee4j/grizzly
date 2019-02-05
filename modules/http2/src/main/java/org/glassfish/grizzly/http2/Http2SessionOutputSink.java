/*
 * Copyright (c) 2014, 2019 Oracle and/or its affiliates and others
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
 *
 * Contributors:
 *   Payara Services - Moved internal OutputQueueRecord into new class.
 */

package org.glassfish.grizzly.http2;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.WriteHandler;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.asyncqueue.AsyncQueueRecord;
import org.glassfish.grizzly.asyncqueue.MessageCloner;
import org.glassfish.grizzly.asyncqueue.TaskQueue;
import org.glassfish.grizzly.http2.frames.DataFrame;
import org.glassfish.grizzly.http2.frames.ErrorCode;
import org.glassfish.grizzly.http2.frames.Http2Frame;
import org.glassfish.grizzly.http2.utils.ChunkedCompletionHandler;

/**
 * Class represents an output sink associated with specific {@link Http2Session}
 * and is responsible for session (connection) level flow control.
 * 
 * @author Alexey Stashok
 */
public class Http2SessionOutputSink {
    protected final Http2Session http2Session;
    private static final Logger LOGGER = Grizzly.logger(Http2SessionOutputSink.class);
    private static final Level LOGGER_LEVEL = Level.FINE;

    private static final int MAX_FRAME_PAYLOAD_SIZE = 16383;
    private static final int MAX_OUTPUT_QUEUE_SIZE = 65536;

    // async output queue
    private final TaskQueue<Http2OutputQueueRecord> outputQueue =
            TaskQueue.createTaskQueue(new TaskQueue.MutableMaxQueueSize() {

                @Override
                public int getMaxQueueSize() {
                    return MAX_OUTPUT_QUEUE_SIZE;
                }
            });

    private final AtomicInteger availConnectionWindowSize;
    private final List<Http2Frame> tmpFramesList = new LinkedList<>();
    private final AtomicBoolean writerLock = new AtomicBoolean();

    public Http2SessionOutputSink(Http2Session session) {
        this.http2Session = session;
        availConnectionWindowSize = new AtomicInteger(
                http2Session.getDefaultConnectionWindowSize());
    }

    protected Http2FrameCodec frameCodec() {
        return http2Session.handlerFilter.frameCodec;
    }
    
    protected void writeDownStream(final Http2Frame frame) {
        
        http2Session.getHttp2SessionChain().write(
                http2Session.getConnection(), null,
                frameCodec().serializeAndRecycle(http2Session, frame),
                null, (MessageCloner) null);
    }

    protected void writeDownStream(final List<Http2Frame> frames) {
        
        http2Session.getHttp2SessionChain().write(
                http2Session.getConnection(), null,
                frameCodec().serializeAndRecycle(http2Session, frames),
                null, (MessageCloner) null);
    }
    
    @SuppressWarnings("unchecked")
    protected <K> void writeDownStream(final K anyMessage,
            final CompletionHandler<WriteResult> completionHandler,
            final MessageCloner<Buffer> messageCloner) {
        
        // Encode Http2Frame -> Buffer
        final Object msg;
        if (anyMessage instanceof List) {
            msg = frameCodec().serializeAndRecycle(
                    http2Session, (List<Http2Frame>) anyMessage);
        } else if (anyMessage instanceof Http2Frame) {
            msg = frameCodec().serializeAndRecycle(
                    http2Session, (Http2Frame) anyMessage);
        } else {
            msg = anyMessage;
        }
        
        http2Session.getHttp2SessionChain().write(http2Session.getConnection(),
                null, msg, completionHandler, messageCloner);        
    }

    protected int getAvailablePeerConnectionWindowSize() {
        return availConnectionWindowSize.get();
    }

    protected boolean canWrite() {
        return outputQueue.size() < MAX_OUTPUT_QUEUE_SIZE;
    }

    protected void notifyCanWrite(final WriteHandler writeHandler) {
        outputQueue.notifyWritePossible(writeHandler, MAX_OUTPUT_QUEUE_SIZE);
    }

    protected void onPeerWindowUpdate(final int delta) throws Http2SessionException {
        final int currentWindow = availConnectionWindowSize.get();
        if (delta > 0 && currentWindow > 0 && currentWindow + delta < 0) {
            throw new Http2SessionException(ErrorCode.FLOW_CONTROL_ERROR, "Session flow-control window overflow.");
        }
        final int newWindowSize = availConnectionWindowSize.addAndGet(delta);
        if (LOGGER.isLoggable(LOGGER_LEVEL)) {
            LOGGER.log(LOGGER_LEVEL, "Http2Session. Expand connection window size by {0} bytes. Current connection window size is: {1}",
                    new Object[] {delta, newWindowSize});
        }

        flushOutputQueue();
    }

    protected void writeDataDownStream(final Http2Stream stream,
                                       final List<Http2Frame> headerFrames,
                                       Buffer data,
                                       final CompletionHandler<WriteResult> completionHandler,
                                       final MessageCloner<Buffer> messageCloner,
                                       final boolean isLast) {

        if (data == null ||
                (!data.hasRemaining() && stream.getUnflushedWritesCount() == 1)) {
            // if there's no data - write now.
            // if there's zero-data and it's the only pending write for the stream - write now.
            if (data == null) {
                writeDownStream(headerFrames, completionHandler, messageCloner);
                return;
            }

            final DataFrame dataFrame = DataFrame.builder()
                    .streamId(stream.getId())
                    .data(data).endStream(isLast)
                    .build();

            final Object msg;
            if (headerFrames != null && !headerFrames.isEmpty()) {
                headerFrames.add(dataFrame);
                msg = headerFrames;
            } else {
                msg = dataFrame;
            }

            writeDownStream(msg, completionHandler, messageCloner);

            return;
        } else if (headerFrames != null && !headerFrames.isEmpty()) {
            // flush the headers now in this thread,
            // because we have to keep compression state consistent
            writeDownStream(headerFrames);
        }

        final int dataSize = data.remaining();

        if (messageCloner != null) {
            data = messageCloner.clone(http2Session.getConnection(), data);
        }

        final Http2OutputQueueRecord record = new Http2OutputQueueRecord(
                stream.getId(), data,
                completionHandler, isLast);

        outputQueue.offer(record);
        outputQueue.reserveSpace(record.isZeroSizeData() ? 1 : dataSize);

        flushOutputQueue();
    }

    private void flushOutputQueue() {
        int backoffDelay = 0;

        int availWindowSize;
        int queueSize;

        boolean needToNotify = false;

        // for debug purposes only
        int tmpcnt = 0;

        // try to flush entire output queue

        // relaxed check if we have free window space and output queue is not empty
        // if yes - lock the writer (only one thread can flush)
        while (availConnectionWindowSize.get() > 0
                && !outputQueue.isEmpty()
                && writerLock.compareAndSet(false, true)) {

            // get the values after the writer is locked
            availWindowSize = availConnectionWindowSize.get();
            queueSize = outputQueue.size();

            CompletionHandler<WriteResult> writeCompletionHandler = null;
            int writeCompletionHandlerBytes = 0;

            int bytesToTransfer = 0;
            int queueSizeToFree = 0;

            AggrCompletionHandler completionHandlers = null;

            // gather all available output data frames
            while (availWindowSize > bytesToTransfer &&
                    queueSize > queueSizeToFree) {

                final Http2OutputQueueRecord record = outputQueue.poll();

                if (record == null) {
                    // keep this warning for now
                    // should be reported when null record is spotted
                    LOGGER.log(Level.WARNING, "UNEXPECTED NULL RECORD. Queue-size: {0} "
                                    + "tmpcnt={1} byteToTransfer={2} queueSizeToFree={3} queueSize={4}",
                            new Object[]{outputQueue.size(), tmpcnt, bytesToTransfer, queueSizeToFree, queueSize});
                }

                assert record != null;

                final int serializedBytes = record.serializeTo(
                        tmpFramesList,
                        Math.min(MAX_FRAME_PAYLOAD_SIZE, availWindowSize - bytesToTransfer));
                bytesToTransfer += serializedBytes;
                queueSizeToFree += serializedBytes;

                if (record.isFinished()) {
                    if (record.isZeroSizeData()) {
                        queueSizeToFree++;
                    }
                } else {
                    outputQueue.setCurrentElement(record);
                }

                final CompletionHandler<WriteResult> recordCompletionHandler =
                        record.getCompletionHandler();

                // add this record CompletionHandler to the list of
                // CompletionHandlers to be notified once all the frames are
                // written
                if (recordCompletionHandler != null) {
                    if (completionHandlers != null) {
                        completionHandlers.register(recordCompletionHandler,
                                serializedBytes);
                    } else if (writeCompletionHandler == null) {
                        writeCompletionHandler = recordCompletionHandler;
                        writeCompletionHandlerBytes = serializedBytes;
                    } else {
                        completionHandlers = new AggrCompletionHandler();
                        completionHandlers.register(writeCompletionHandler,
                                writeCompletionHandlerBytes);
                        completionHandlers.register(recordCompletionHandler,
                                serializedBytes);
                        writeCompletionHandler = completionHandlers;
                    }
                }
            }

            // if at least one byte was consumed from the output queue
            if (queueSizeToFree > 0) {
                assert !tmpFramesList.isEmpty();

                // write the frame list
                writeDownStream(tmpFramesList, writeCompletionHandler, null);

                final int newWindowSize =
                        availConnectionWindowSize.addAndGet(-bytesToTransfer);

                outputQueue.releaseSpace(queueSizeToFree);

                needToNotify = true;
                if (LOGGER.isLoggable(LOGGER_LEVEL)) {
                    LOGGER.log(LOGGER_LEVEL, "Http2Session. Shrink connection window size by {0} bytes. Current connection window size is: {1}",
                            new Object[] {bytesToTransfer, newWindowSize});
                }

            }

            // release the writer lock, so other thread can start to write
            writerLock.set(false);

            // we don't want this thread to write all the time - so give more
            // time for another thread to start writing
            LockSupport.parkNanos(backoffDelay++);
            tmpcnt++;
        }

        if (needToNotify) {
            outputQueue.doNotify();
        }
    }

    public void close() {
        outputQueue.onClose();
    }

}
