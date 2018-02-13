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

package org.glassfish.grizzly.http.server.filecache;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.util.ContentType;

/**
 * The entry value in the file cache map.
 *
 * @author Alexey Stashok
 */
public final class FileCacheEntry implements Runnable {

    private static final Logger LOGGER = Grizzly.logger(FileCacheEntry.class);

    public FileCacheKey key;
    public String host;
    public String requestURI;
    public long lastModified = -1;
    public ContentType contentType;
    ByteBuffer bb;
    // The reference to the plain file to be served
    File plainFile;
    long plainFileSize = -1;
    
    private boolean canBeCompressed;
    private AtomicBoolean isCompressed;
    volatile File compressedFile;
    ByteBuffer compressedBb;
    long compressedFileSize = -1;
    
    public String xPoweredBy;
    public FileCache.CacheType type;
    public String date;
    public String Etag;
    public String lastModifiedHeader;
    public String server;

    public volatile long timeoutMillis;

    private final FileCache fileCache;

    public FileCacheEntry(FileCache fileCache) {
        this.fileCache = fileCache;
    }

    /**
     * <tt>true</tt> means this entry could be served compressed, if client
     * supports compression, or <tt>false</tt> if this entry should be always
     * served as it is.
     */
    void setCanBeCompressed(final boolean canBeCompressed) {
        this.canBeCompressed = canBeCompressed;
        
        if (canBeCompressed) {
            isCompressed = new AtomicBoolean();
        }
    }
    
    /**
     * Returns <tt>true</tt> if this entry could be served compressed as response
     * to this (passed) specific {@link HttpRequestPacket}. Or <tt>false</tt>
     * will be returned otherwise.
     */
    public boolean canServeCompressed(final HttpRequestPacket request) {
        if (!canBeCompressed ||
                !CompressionConfig.isClientSupportCompression(
                fileCache.getCompressionConfig(), request,
                FileCache.COMPRESSION_ALIASES)) {
            return false;
        }
        
        if (isCompressed.compareAndSet(false, true)) {
            fileCache.compressFile(this);
        }
        
        // compressedFile could be still "null" if the file compression was
        // initiated by other request and it is still not completed
        return compressedFile != null;
    }
    
    /**
     * Returns the entry file size.
     * @param isCompressed if <tt>true</tt> the compressed file size will be
     *        returned, otherwise uncompressed file size will be returned as the result.
     * @return the entry file size
     */
    public long getFileSize(final boolean isCompressed) {
        return isCompressed ? compressedFileSize : plainFileSize;
    }
    
    /**
     * Returns the entry's {@link File} reference.
     * @param isCompressed if <tt>true</tt> the compressed {@link File} reference
     *        will be returned, otherwise uncompressed {@link File} reference will
     *        be returned as the result.
     * @return the entry's {@link File} reference
     */
    public File getFile(final boolean isCompressed) {
        return isCompressed ? compressedFile : plainFile;
    }
    
    /**
     * Returns the entry's {@link ByteBuffer} representation.
     * @param isCompressed if <tt>true</tt> the compressed {@link ByteBuffer}
     *        will be returned, otherwise uncompressed {@link ByteBuffer} will
     *        be returned as the result.
     * @return the entry's {@link ByteBuffer} reference
     */
    public ByteBuffer getByteBuffer(final boolean isCompressed) {
        return isCompressed ? compressedBb : bb;
    }
    
    @Override
    public void run() {
        fileCache.remove(this);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("FileCacheEntry");
        sb.append("{host='").append(host).append('\'');
        sb.append(", requestURI='").append(requestURI).append('\'');
        sb.append(", lastModified=").append(lastModified);
        sb.append(", contentType='").append(contentType).append('\'');
        sb.append(", type=").append(type);
        sb.append(", plainFileSize=").append(plainFileSize);
        sb.append(", canBeCompressed=").append(canBeCompressed);
        sb.append(", compressedFileSize=").append(compressedFileSize);
        sb.append(", timeoutMillis=").append(timeoutMillis);
        sb.append(", fileCache=").append(fileCache);
        sb.append(", server=").append(server);
        sb.append('}');
        return sb.toString();
    }

    @Override
    protected void finalize() throws Throwable {
        if (compressedFile != null) {
            if (!compressedFile.delete()) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE,
                               "Unable to delete file {0}.  Will try to delete again upon VM exit.",
                               compressedFile.getCanonicalPath());
                }
                compressedFile.deleteOnExit();
            }
        }
        
        super.finalize();
    }
}
