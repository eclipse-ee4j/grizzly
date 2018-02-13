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

package org.glassfish.grizzly.http;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;

/**
 * Utility class, which has notification methods for different
 * {@link HttpProbe} events.
 *
 * @author Alexey Stashok
 */
final class HttpProbeNotifier {

    /**
     * Notify registered {@link HttpProbe}s about the "data received" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param buffer {@link Buffer}.
     */
    static void notifyDataReceived(final HttpCodecFilter httpFilter,
            final Connection connection,
            final Buffer buffer) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onDataReceivedEvent(connection, buffer);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "data sent" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param buffer {@link Buffer}.
     */
    static void notifyDataSent(final HttpCodecFilter httpFilter,
            final Connection connection,
            final Buffer buffer) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onDataSentEvent(connection, buffer);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "header parsed" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}s been parsed.
     * @param size the size of the parsed header buffer.
     */
    static void notifyHeaderParse(final HttpCodecFilter httpFilter,
            final Connection connection,
            final HttpHeader header, final int size) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onHeaderParseEvent(connection, header, size);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "header serialized" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}s been serialized.
     * @param buffer the serialized header {@link Buffer}.
     */
    static void notifyHeaderSerialize(final HttpCodecFilter httpFilter,
            final Connection connection, final HttpHeader header,
            final Buffer buffer) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onHeaderSerializeEvent(connection, header, buffer);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "content chunk parsed" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param content HTTP {@link HttpContent}s been parsed.
     */
    static void notifyContentChunkParse(final HttpCodecFilter httpFilter,
            final Connection connection,
            final HttpContent content) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onContentChunkParseEvent(connection, content);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "content chunk serialize" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param content HTTP {@link HttpContent}s to be serialized.
     */
    static void notifyContentChunkSerialize(final HttpCodecFilter httpFilter,
            final Connection connection,
            final HttpContent content) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onContentChunkSerializeEvent(connection, content);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "content encoding parse" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}, the event belongs to.
     * @param buffer {@link Buffer} to be parsed/decoded.
     * @param contentEncoding {@link ContentEncoding} to be applied for parsing.
     */
    static void notifyContentEncodingParse(final HttpCodecFilter httpFilter,
            final Connection connection, final HttpHeader header,
            final Buffer buffer, final ContentEncoding contentEncoding) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onContentEncodingParseEvent(connection, header, buffer,
                        contentEncoding);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the result of the "content encoding decode" event.
     *
     * @param httpFilter      the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection      the <tt>Connection</tt> event occurred on.
     * @param header          HTTP {@link HttpHeader}, the event belongs to.
     * @param result          the result of the decoding process.
     * @param contentEncoding the {@link ContentEncoding} which was applied.
     * @since 2.3.3
     */
    static void notifyContentEncodingParseResult(final HttpCodecFilter httpFilter,
                                                 final Connection connection,
                                                 final HttpHeader header,
                                                 final Buffer result,
                                                 final ContentEncoding contentEncoding) {
        final HttpProbe[] probes =
                httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onContentEncodingSerializeResultEvent(connection,
                                                            header,
                                                            result,
                                                            contentEncoding);
            }
        }
    }
    
    /**
     * Notify registered {@link HttpProbe}s about the "content encoding serialize" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}, the event belongs to.
     * @param buffer {@link Buffer} to be serialized/encoded.
     * @param contentEncoding {@link ContentEncoding} to be applied for serializing.
     */
    static void notifyContentEncodingSerialize(final HttpCodecFilter httpFilter,
            final Connection connection, final HttpHeader header,
            final Buffer buffer, final ContentEncoding contentEncoding) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onContentEncodingSerializeEvent(connection, header, buffer,
                        contentEncoding);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the result of the "content encoding serialize" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}, the event belongs to.
     * @param result the result of the encoding process.
     * @param contentEncoding the {@link ContentEncoding} which was applied.
     *
     * @since 2.3.3
     */
    static void notifyContentEncodingSerializeResult(final HttpCodecFilter httpFilter,
                                                     final Connection connection,
                                                     final HttpHeader header,
                                                     final Buffer result,
                                                     final ContentEncoding contentEncoding) {
        final HttpProbe[] probes =
                httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onContentEncodingSerializeResultEvent(connection,
                                                            header,
                                                            result,
                                                            contentEncoding);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "transfer encoding parse" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}, the event belongs to.
     * @param buffer {@link Buffer} to be parsed/decoded.
     * @param transferEncoding {@link TransferEncoding} to be applied for parsing.
     */
    static void notifyTransferEncodingParse(final HttpCodecFilter httpFilter,
            final Connection connection, final HttpHeader header,
            final Buffer buffer, final TransferEncoding transferEncoding) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onTransferEncodingParseEvent(connection, header, buffer,
                        transferEncoding);
            }
        }
    }

    /**
     * Notify registered {@link HttpProbe}s about the "transfer encoding serialize" event.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param header HTTP {@link HttpHeader}, the event belongs to.
     * @param buffer {@link Buffer} to be serialized/encoded.
     * @param transferEncoding {@link TransferEncoding} to be applied for serializing.
     */
    static void notifyTransferEncodingSerialize(final HttpCodecFilter httpFilter,
            final Connection connection, final HttpHeader header,
            final Buffer buffer, final TransferEncoding transferEncoding) {

        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            for (HttpProbe probe : probes) {
                probe.onTransferEncodingSerializeEvent(connection, header, buffer,
                        transferEncoding);
            }
        }
    }
    
    /**
     * Notify registered {@link HttpProbe}s about the error.
     *
     * @param httpFilter the <tt>HttpCodecFilter</tt> event occurred on.
     * @param connection the <tt>Connection</tt> event occurred on.
     * @param httpPacket the <tt>HttpPacket</tt> event occurred on.
     * @param error {@link Throwable}.
     */
    static void notifyProbesError(final HttpCodecFilter httpFilter,
            final Connection connection,
            final HttpPacket httpPacket,
            Throwable error) {
        final HttpProbe[] probes = httpFilter.monitoringConfig.getProbesUnsafe();
        if (probes != null) {
            if (error == null) {
                error = new IllegalStateException("Error in HTTP semantics");
            }
            
            for (HttpProbe probe : probes) {
                probe.onErrorEvent(connection, httpPacket, error);
            }
        }
    }
}
