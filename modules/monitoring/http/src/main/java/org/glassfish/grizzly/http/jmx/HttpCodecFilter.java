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

package org.glassfish.grizzly.http.jmx;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.http.ContentEncoding;
import org.glassfish.grizzly.http.GZipContentEncoding;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpProbe;
import org.glassfish.grizzly.http.LZMAContentEncoding;
import org.glassfish.grizzly.http.TransferEncoding;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

import java.util.concurrent.atomic.AtomicLong;

import org.glassfish.grizzly.http.HttpPacket;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * JMX management object for the {@link HttpCodecFilter}.
 *
 * @since 2.0
 */
@ManagedObject
@Description("This Filter is responsible for the parsing incoming HTTP packets and serializing high level objects back into the HTTP protocol format.")
public class HttpCodecFilter extends JmxObject {

    private final org.glassfish.grizzly.http.HttpCodecFilter httpCodecFilter;

    private final AtomicLong httpContentReceived = new AtomicLong();
    private final AtomicLong httpContentWritten = new AtomicLong();
    private final AtomicLong httpCodecErrorCount = new AtomicLong();
    private final AtomicLong contentCompressionTotalGzip = new AtomicLong();
    private final AtomicLong contentBeforeCompressionTotalGzip = new AtomicLong();
    private final AtomicLong contentCompressionTotalLzma = new AtomicLong();
    private final AtomicLong contentBeforeCompressionTotalLzma = new AtomicLong();

    private final HttpProbe probe = new JmxHttpProbe();


    // ------------------------------------------------------------ Constructors


    public HttpCodecFilter(org.glassfish.grizzly.http.HttpCodecFilter httpCodecFilter) {
        this.httpCodecFilter = httpCodecFilter;
    }


    // -------------------------------------------------- Methods from JmxObject


    /**
     * {@inheritDoc}
     */
    @Override
    public String getJmxName() {
        return "HttpCodecFilter";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
        httpCodecFilter.getMonitoringConfig().addProbes(probe);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDeregister(GrizzlyJmxManager mom) {
        httpCodecFilter.getMonitoringConfig().removeProbes(probe);
    }


    // -------------------------------------------------------------- Attributes


    /**
     * @return total number of bytes received by this
     *  {@link org.glassfish.grizzly.http.HttpCodecFilter}.
     */
    @ManagedAttribute(id="total-bytes-received")
    @Description("The total number of bytes this filter has processed as part of the HTTP protocol parsing process.")
    public long getTotalContentReceived() {
        return httpContentReceived.get();
    }


    /**
     * @return total number of bytes written by this
     *  {@link org.glassfish.grizzly.http.HttpCodecFilter}.
     */
    @ManagedAttribute(id="total-bytes-written")
    @Description("The total number of bytes that have been written as part of the serialization process to the HTTP protocol.")
    public long getTotalContentWritten() {
        return httpContentWritten.get();
    }


    /**
     * @return total number of HTTP codec errors.
     */
    @ManagedAttribute(id="http-codec-error-count")
    @Description("The total number of protocol errors that have occurred during either the parsing or serialization process.")
    public long getHttpCodecErrorCount() {
        return httpCodecErrorCount.get();
    }

    /**
     * @return total number of bytes sent to gzip to be compressed.
     */
    @ManagedAttribute(id="http-codec-before-gzip-compression-total")
    @Description("The total number of bytes before gzip compression has been applied.")
    public long getTotalBytesBeforeGzipEncoding() {
        return contentBeforeCompressionTotalGzip.get();
    }

    /**
     * @return total number of bytes after gzip compression.
     */
    @ManagedAttribute(id="http-codec-after-gzip-compression-total")
    @Description("The total number of bytes after gzip compression has been applied.")
    public long getTotalBytesAfterGzipEncoding() {
        return contentCompressionTotalGzip.get();
    }

    /**
     * @return the gzip compression ratio.
     */
    @ManagedAttribute(id="http-codec-gzip-avg-compression-percent")
    @Description("The average gzip compression result.")
    public String getGzipCompressionRatio() {
        final long l1 = contentBeforeCompressionTotalGzip.get();
        final long l2 = contentCompressionTotalGzip.get();
        return calculateAvgCompressionPercent(l1, l2);
    }

    /**
     * @return total number of bytes sent to lzma be compressed.
     */
    @ManagedAttribute(id = "http-codec-before-lzma-compression-total")
    @Description( "The total number of bytes before lzma compression has been applied.")
    public long getTotalBytesBeforeLzmaEncoding() {
        return contentBeforeCompressionTotalLzma.get();
    }

    /**
     * @return total number of bytes after lzma compression.
     */
    @ManagedAttribute(id = "http-codec-after-lzma-compression-total")
    @Description( "The total number of bytes after lzma compression has been applied.")
    public long getTotalBytesAfterLzmaEncoding() {
        return contentCompressionTotalLzma.get();
    }

    /**
     * @return the lzma compression ratio.
     */
    @ManagedAttribute(id = "http-codec-lzma-avg-compression-percent")
    @Description( "The average lzma compression result.")
    public String getLzmaAvgCompressionPercent() {
        final long l1 = contentBeforeCompressionTotalLzma.get();
        final long l2 = contentCompressionTotalLzma.get();
        return calculateAvgCompressionPercent(l1, l2);
    }


    // --------------------------------------------------------- Private Methods


    private String calculateAvgCompressionPercent(double original, double result) {
        double r = 100 - ((result / original) * 100);

        return String.format("%.2f%%", r);
    }


    // ---------------------------------------------------------- Nested Classes


    private final class JmxHttpProbe implements HttpProbe {

        @Override
        public void onDataReceivedEvent(Connection connection, Buffer buffer) {
            httpContentReceived.addAndGet(buffer.remaining());
        }

        @Override
        public void onDataSentEvent(Connection connection, Buffer buffer) {
            httpContentWritten.addAndGet(buffer.remaining());
        }

        @Override
        public void onErrorEvent(Connection connection, HttpPacket httpPacket,
                Throwable error) {
            httpCodecErrorCount.incrementAndGet();
        }

        @Override
        public void onHeaderParseEvent(Connection connection, HttpHeader header,
                int size) {
        }

        @Override
        public void onHeaderSerializeEvent(Connection connection, HttpHeader header, Buffer buffer) {
        }

        @Override
        public void onContentChunkParseEvent(Connection connection, HttpContent content) {
        }

        @Override
        public void onContentEncodingParseResultEvent(Connection connection, HttpHeader header, Buffer result, ContentEncoding contentEncoding) {

        }

        @Override
        public void onContentChunkSerializeEvent(Connection connection, HttpContent content) {
        }

        @Override
        public void onContentEncodingSerializeResultEvent(Connection connection, HttpHeader header, Buffer result, ContentEncoding contentEncoding) {
            final String name = contentEncoding.getName();
            if (GZipContentEncoding.NAME.equals(name)) {
                contentCompressionTotalGzip.addAndGet(result.remaining());
            } else if (LZMAContentEncoding.NAME.equals(name)) {
                contentCompressionTotalLzma.addAndGet(result.remaining());
            }
        }

        @Override
        public void onContentEncodingParseEvent(Connection connection, HttpHeader header, Buffer buffer, ContentEncoding contentEncoding) {
        }

        @Override
        public void onContentEncodingSerializeEvent(Connection connection, HttpHeader header, Buffer result, ContentEncoding contentEncoding) {
            final String name = contentEncoding.getName();
            if (GZipContentEncoding.NAME.equals(name)) {
                contentBeforeCompressionTotalGzip.addAndGet(result.remaining());
            } else if (LZMAContentEncoding.NAME.equals(name)) {
                contentBeforeCompressionTotalLzma.addAndGet(result.remaining());
            }
        }

        @Override
        public void onTransferEncodingParseEvent(Connection connection, HttpHeader header, Buffer buffer, TransferEncoding transferEncoding) {
        }

        @Override
        public void onTransferEncodingSerializeEvent(Connection connection, HttpHeader header, Buffer buffer, TransferEncoding transferEncoding) {
        }

    } // End JmxHttpProbe

}
