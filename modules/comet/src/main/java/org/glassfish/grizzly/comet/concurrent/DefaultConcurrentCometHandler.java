/*
 * Copyright (c) 2009, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.comet.concurrent;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import org.glassfish.grizzly.comet.CometContext;
import org.glassfish.grizzly.comet.CometEvent;
import org.glassfish.grizzly.comet.CometHandler;
import org.glassfish.grizzly.http.server.Response;

/**
 * We queue events in each CometHandler to lower the probability that slow or massive IO for one CometHandler severely
 * delays events to others.<br>
 * <br>
 * only streaming mode can benefit from buffering messages like this. <br>
 * only 1 thread at a time is allowed to do IO, other threads put events in the queue and return to the thread pool.<br>
 * <br>
 * a thread initially calls enqueueEvent and stay there until there are no more events in the queue, calling the onEVent
 * method in synchronized context for each Event.<br>
 * <br>
 * on IOE in onEvent we terminate.<br>
 * we have a limit, to keep memory usage under control.<br>
 * <br>
 * if queue limit is reached onQueueFull is called, and then we terminate.<br>
 * <br>
 * <br>
 * whats not optimal is that a worker thread is sticky to the client depending upon available events in the handlers
 * local queue, that can in theory allow a few clients to block all threads for extended time.<br>
 * that effect can make this implementation unusable depending on the scenario, its not a perfect design be any means.
 * <br>
 * The potential improvement is that only 1 worker thread is tied up to a client instead of several being blocked by
 * synchronized io wait for one CometHandler .<br>
 *
 * @author Gustav Trede
 */
public abstract class DefaultConcurrentCometHandler<E> implements CometHandler<E> {
    protected final static Logger logger = Logger.getLogger(DefaultConcurrentCometHandler.class.getName());
    /**
     * used for preventing the worker threads from the executor event queue from adding events to the comet handlers local
     * queue or starting IO logic after shut down.<br>
     */
    private boolean shuttingDown;
    /**
     * max number of events to locally queue for this CometHandler.<br>
     * (a global event queue normally exists in form of a thread pool too)
     */
    private final int messageQueueLimit;
    /**
     * current queue size
     */
    private int queueSize;
    /**
     * true means that no thread is currently active on this comet handlers queue logic
     */
    private boolean readyForWork = true;
    /**
     * todo replace with non array copying list for non resizing add situations, using internal index to keep track of state
     * , not a linked list, it has too much overhead and eats memory.
     */
    protected final Queue<CometEvent> messageQueue = new LinkedList<>();
    private CometContext<E> context;
    private Response response;

    public DefaultConcurrentCometHandler(final CometContext<E> context, final Response response) {
        this(100);
        this.context = context;
        this.response = response;
    }

    /**
     * @param messageQueueLimit
     */
    public DefaultConcurrentCometHandler(int messageQueueLimit) {
        this.messageQueueLimit = messageQueueLimit;
    }

    @Override
    public CometContext<E> getCometContext() {
        return context;
    }

    @Override
    public void setCometContext(final CometContext<E> context) {
        this.context = context;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setResponse(final Response response) {
        this.response = response;
    }

    /**
     * Queues event if another thread is currently working on this handler. The first thread to start working will keep
     * doing so until there are no further events in the internal queue.
     */
    public void enqueueEvent(CometEvent event) throws IOException {
        synchronized (messageQueue) {
            if (!readyForWork) {
                if (!shuttingDown && queueSize < messageQueueLimit) {
                    messageQueue.add(event);
                    queueSize++;
                }
                return;
            }
            readyForWork = false;
        }
        boolean queueFull = false;
        while (!shuttingDown) {
            if (!event.getCometContext().isActive(this)) {
                shuttingDown = true;
                return;
            }
            try {
                // move synchronized outside the while loop ?
                synchronized (this) {
                    onEvent(event);
                }
            } catch (IOException ex) {
                shuttingDown = true;
            } finally {
                if (shuttingDown) {
                    event.getCometContext().resumeCometHandler(this);
                    return;
                }
            }
            synchronized (messageQueue) {
                if (queueSize == messageQueueLimit) {
                    queueFull = true;
                } else if (queueSize == 0) {
                    readyForWork = true;
                    return;
                } else {
                    event = messageQueue.poll();
                    queueSize--;
                }
            }
            if (queueFull) {
                shuttingDown = true;
                // todo is onQueueFull needed ? or just terminate, it would simplify to just terminate =)
                onQueueFull(event);
            }
        }
    }

    /**
     * Called in synchronized context when the comet handler's local event queue is full.<br>
     * default impl resumes the comet handler
     *
     * @param event {@link CometEvent}
     */
    public void onQueueFull(CometEvent event) throws IOException {
        event.getCometContext().resumeCometHandler(this);
    }

    /**
     * {@inheritDoc} <br>
     * default impl calls terminate()
     */
    @Override
    public void onInterrupt(CometEvent event) throws IOException {
        terminate();
    }

    /**
     * {@inheritDoc} <br>
     * default impl calls terminate()
     */
    @Override
    public void onTerminate(CometEvent event) throws IOException {
        terminate();
    }

    protected void terminate() {
    }
}
