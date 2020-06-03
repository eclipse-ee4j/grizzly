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

package org.glassfish.grizzly.http.server;

import java.util.Arrays;

import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.CompressionConfig.CompressionMode;
import org.glassfish.grizzly.http.CompressionConfig.CompressionModeI;
import org.glassfish.grizzly.http.EncodingFilter;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.util.DataChunk;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.grizzly.http.util.MimeHeaders;

public class CompressionEncodingFilter implements EncodingFilter {
    private final CompressionConfig compressionConfig;
    private final String[] aliases;

    public CompressionEncodingFilter(final CompressionConfig compressionConfig, final String[] aliases) {
        this.compressionConfig = new CompressionConfig(compressionConfig);
        this.aliases = Arrays.copyOf(aliases, aliases.length);
    }

    /**
     * Creates a new CompressionEncodingFilter based on the provided configuration details.
     *
     * @param compressionMode is compression on, off, or forced.
     * @param compressionMinSize the minimum size, in bytes, the resource must be before being considered for compression.
     * @param compressibleMimeTypes resource mime types that may be compressed. if null or zero-length, then there will be
     * no type restriction.
     * @param noCompressionUserAgents user agents for which compression will not be performed. If null or zero-length, the
     * user agent will not be considered.
     * @param aliases aliases for the compression name as defined in the accept-encoding header of the request.
     */
    public CompressionEncodingFilter(CompressionModeI compressionMode, int compressionMinSize, String[] compressibleMimeTypes, String[] noCompressionUserAgents,
            String[] aliases) {

        this(compressionMode, compressionMinSize, compressibleMimeTypes, noCompressionUserAgents, aliases, false);
    }

    /**
     * Creates a new CompressionEncodingFilter based on the provided configuration details.
     *
     * @param compressionMode is compression on, off, or forced.
     * @param compressionMinSize the minimum size, in bytes, the resource must be before being considered for compression.
     * @param compressibleMimeTypes resource mime types that may be compressed. if null or zero-length, then there will be
     * no type restriction.
     * @param noCompressionUserAgents user agents for which compression will not be performed. If null or zero-length, the
     * user agent will not be considered.
     * @param aliases aliases for the compression name as defined in the accept-encoding header of the request.
     * @param enableDecompression enabled decompression of incoming data according to the content-encoding header
     *
     * @since 2.3.29
     */
    public CompressionEncodingFilter(CompressionModeI compressionMode, int compressionMinSize, String[] compressibleMimeTypes, String[] noCompressionUserAgents,
            String[] aliases, boolean enableDecompression) {

        final CompressionMode mode;
        if (compressionMode instanceof CompressionMode) {
            mode = (CompressionMode) compressionMode;
        } else {
            // backwards compatibility
            assert compressionMode instanceof CompressionLevel;
            mode = ((CompressionLevel) compressionMode).normalize();
        }

        compressionConfig = new CompressionConfig(mode, compressionMinSize, null, null, enableDecompression);
        compressionConfig.setCompressibleMimeTypes(compressibleMimeTypes);
        compressionConfig.setNoCompressionUserAgents(noCompressionUserAgents);

        this.aliases = Arrays.copyOf(aliases, aliases.length);
    }

    @Override
    public boolean applyEncoding(final HttpHeader httpPacket) {
        if (httpPacket.isRequest()) {
            assert httpPacket instanceof HttpRequestPacket;
            return false;
        }

        assert httpPacket instanceof HttpResponsePacket;
        return canCompressHttpResponse((HttpResponsePacket) httpPacket, compressionConfig, aliases);
    }

    @Override
    public boolean applyDecoding(final HttpHeader httpPacket) {
        if (!httpPacket.isRequest()) {
            return false;
        }

        assert httpPacket instanceof HttpRequestPacket;
        return canDecompressHttpRequest((HttpRequestPacket) httpPacket, compressionConfig, aliases);
    }

    /**
     * Returns <tt>true</tt> if the {@link HttpResponsePacket} could be compressed, or <tt>false</tt> otherwise. The method
     * checks if client supports compression and if the resource, that we are about to send matches
     * {@link CompressionConfig} configuration.
     */
    protected static boolean canCompressHttpResponse(final HttpResponsePacket response, final CompressionConfig compressionConfig, final String[] aliases) {

        // If at least one encoding has been already selected
        // skip this one
        if (!response.getContentEncodings().isEmpty()) {
            return false;
        }

        final MimeHeaders responseHeaders = response.getHeaders();
        // Check if content is already encoded (no matter which encoding)
        final DataChunk contentEncodingMB = responseHeaders.getValue(Header.ContentEncoding);
        if (contentEncodingMB != null && !contentEncodingMB.isNull()) {
            return false;
        }

        if (!CompressionConfig.isClientSupportCompression(compressionConfig, response.getRequest(), aliases)) {
            return false;
        }

        // If force mode, always compress (test purposes only)
        if (compressionConfig.getCompressionMode() == CompressionMode.FORCE) {
            response.setChunked(true);
            response.setContentLength(-1);
            return true;
        }

        // Check if sufficient len to trig the compression
        final long contentLength = response.getContentLength();
        if (contentLength == -1 || contentLength >= compressionConfig.getCompressionMinSize()) {

            if (compressionConfig.checkMimeType(response.getContentType())) {
                response.setChunked(true);
                response.setContentLength(-1);
                return true;
            }
        }

        return false;
    }

    /**
     * Returns <tt>true</tt> if the {@link HttpResponsePacket} could be compressed, or <tt>false</tt> otherwise. The method
     * checks if client supports compression and if the resource, that we are about to send matches
     * {@link CompressionConfig} configuration.
     */
    protected static boolean canDecompressHttpRequest(final HttpRequestPacket request, final CompressionConfig config, final String[] aliases) {

        if (!config.isDecompressionEnabled()) {
            return false;
        }

        String contentEncoding = request.getHeader(Header.ContentEncoding);

        // If no header is present, assume request is not encoded, so don't decode
        if (contentEncoding == null) {
            return false;
        }

        // If content encoding is set to one of the aliases supported by this
        // filter, decoding should happen.
        contentEncoding = contentEncoding.trim();
        for (String alias : aliases) {
            if (alias.equals(contentEncoding)) {
                return true;
            }
        }
        return false;
    }
}
