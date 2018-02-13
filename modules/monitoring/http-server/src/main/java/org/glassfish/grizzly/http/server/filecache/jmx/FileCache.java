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

package org.glassfish.grizzly.http.server.filecache.jmx;

import org.glassfish.grizzly.http.server.filecache.FileCacheEntry;
import org.glassfish.grizzly.http.server.filecache.FileCacheProbe;
import org.glassfish.grizzly.monitoring.jmx.JmxObject;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.GmbalMBean;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;

/**
 * This class provides a JMX view of the current operating state of the
 * FileCache.
 *
 * @since 2.0
 */
@ManagedObject
@Description("Static file caching implementation.  There will be one FileCache instance per NetworkListener.")
public class FileCache extends JmxObject {

    /**
     * The {@link org.glassfish.grizzly.http.server.filecache.FileCache} being managed.
     */
    private final org.glassfish.grizzly.http.server.filecache.FileCache fileCache;

    /**
     * The current {@link org.glassfish.grizzly.http.server.filecache.FileCache} entry count.
     */
    private final AtomicInteger cachedEntryCount = new AtomicInteger();

    /**
     * The number of cache hits.
     */
    private final AtomicLong cacheHitCount = new AtomicLong();

    /**
     * The number of cache misses.
     */
    private final AtomicLong cacheMissCount = new AtomicLong();

    /**
     * The number of cache errors.
     */
    private final AtomicInteger cacheErrorCount = new AtomicInteger();

    /**
     * The {@link FileCacheProbe} used to track cache statistics.
     */
    private final JMXFileCacheProbe fileCacheProbe = new JMXFileCacheProbe();



    // ------------------------------------------------------------ Constructors


    /**
     * Constructs a new JMX managed FileCache for the specified
     * {@link org.glassfish.grizzly.http.server.filecache.FileCache} instance.
     *
     * @param fileCache the {@link org.glassfish.grizzly.http.server.filecache.FileCache}
     *  to manage.
     */
    public FileCache(org.glassfish.grizzly.http.server.filecache.FileCache fileCache) {
        this.fileCache = fileCache;
    }


    // -------------------------------------------------- Methods from JmxObject


    /**
     * {@inheritDoc}
     */
    @Override
    public String getJmxName() {
        return "FileCache";
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * <p>
     * When invoked, this method will add a {@link FileCacheProbe} to track
     * statistics.
     * </p>
     */
    @Override
    protected void onRegister(GrizzlyJmxManager mom, GmbalMBean bean) {
        fileCache.getMonitoringConfig().addProbes(fileCacheProbe);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * <p>
     * When invoked, this method will remove the {@link FileCacheProbe} added
     * by the {@link #onRegister(org.glassfish.grizzly.monitoring.jmx.GrizzlyJmxManager, org.glassfish.gmbal.GmbalMBean)}
     * call.
     * </p>
     */
    @Override
    protected void onDeregister(GrizzlyJmxManager mom) {
        fileCache.getMonitoringConfig().removeProbes(fileCacheProbe);
    }


    // --------------------------------------------------- File Cache Properties


    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#isEnabled()
     */
    @ManagedAttribute(id="file-cache-enabled")
    @Description("Indicates whether or not the file cache is enabled.")
    public boolean isFileCacheEnabled() {
        return fileCache.isEnabled();
    }

    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#getSecondsMaxAge()
     */
    @ManagedAttribute(id="max-age-seconds")
    @Description("The maximum age, in seconds, a resource may be cached.")
    public int getSecondsMaxAge() {
        return fileCache.getSecondsMaxAge();
    }

    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#getMaxCacheEntries()
     */
    @ManagedAttribute(id="max-number-of-cache-entries")
    @Description("The maxumim number of entries that may exist in the cache.")
    public int getMaxCacheEntries() {
        return fileCache.getMaxCacheEntries();
    }

    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#getMinEntrySize()
     */
    @ManagedAttribute(id="min-entry-size")
    @Description("The maximum size, in bytes, a file must be in order to be cached in the heap cache.")
    public long getMinEntrySize() {
        return fileCache.getMinEntrySize();
    }

    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#getMaxEntrySize()
     */
    @ManagedAttribute(id="max-entry-size")
    @Description("The maximum size, in bytes, a resource may be before it can no longer be considered cachable.")
    public long getMaxEntrySize() {
        return fileCache.getMaxEntrySize();
    }

    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#getMaxLargeFileCacheSize()
     */
    @ManagedAttribute(id="memory-mapped-file-cache-size")
    @Description("The maximum size, in bytes, of the memory mapped cache for large files.")
    public long getMaxLargeFileCacheSize() {
        return fileCache.getMaxLargeFileCacheSize();
    }

    /**
     * @see org.glassfish.grizzly.http.server.filecache.FileCache#getMaxSmallFileCacheSize()
     */
    @ManagedAttribute(id="heap-file-cache-size")
    @Description("The maximum size, in bytes, of the heap cache for files below the water mark set by min-entry-size.")
    public long getMaxSmallFileCacheSize() {
        return fileCache.getMaxSmallFileCacheSize();
    }


    /**
     * @return the total number of cached entries.
     */
    @ManagedAttribute(id="cached-entries-count")
    @Description("The current cached entry count.")
    public int getCachedEntryCount() {
        return cachedEntryCount.get();
    }

    /**
     * @return the total number of cache hits.
     */
    @ManagedAttribute(id="cache-hit-count")
    @Description("The total number of cache hits.")
    public long getCacheHitCount() {
        return cacheHitCount.get();
    }

    /**
     * @return the total number of cache misses.
     */
    @ManagedAttribute(id="cache-miss-count")
    @Description("The total number of cache misses.")
    public long getCacheMissCount() {
        return cacheMissCount.get();
    }

    /**
     * @return the total number of cache errors.
     */
    @ManagedAttribute(id="cache-error-count")
    @Description("The total number of cache errors.")
    public int getCacheErrorCount() {
        return cacheErrorCount.get();
    }

    /**
     * @return the total size, in bytes, of the heap memory cache.
     */
    @ManagedAttribute(id="heap-cache-size-in-bytes")
    @Description("The current size, in bytes, of the heap memory cache.")
    public long getHeapMemoryInBytes() {
        return fileCache.getHeapCacheSize();        
    }

    /**
     * @return the total size, in bytes, of the mapped memory cache.
     */
    @ManagedAttribute(id="mapped-memory-cache-size-in-bytes")
    @Description("The current size, in bytes, of the mapped memory cache.")
    public long getMappedMemorytInBytes() {
        return fileCache.getMappedCacheSize();
    }


    // ---------------------------------------------------------- Nested Classes


    /**
     * JMX statistic gathering {@link FileCacheProbe}.
     */
    private final class JMXFileCacheProbe implements FileCacheProbe {


        // ----------------------------------------- Methods from FileCacheProbe


        @Override
        public void onEntryAddedEvent(org.glassfish.grizzly.http.server.filecache.FileCache fileCache, FileCacheEntry entry) {
            cachedEntryCount.incrementAndGet();
        }

        @Override
        public void onEntryRemovedEvent(org.glassfish.grizzly.http.server.filecache.FileCache fileCache, FileCacheEntry entry) {
            if (cachedEntryCount.get() > 0) {
                cachedEntryCount.decrementAndGet();
            }
        }

        @Override
        public void onEntryHitEvent(org.glassfish.grizzly.http.server.filecache.FileCache fileCache, FileCacheEntry entry) {
            cacheHitCount.incrementAndGet();
        }

        @Override
        public void onEntryMissedEvent(org.glassfish.grizzly.http.server.filecache.FileCache fileCache, String host, String requestURI) {
            cacheMissCount.incrementAndGet();
        }

        @Override
        public void onErrorEvent(org.glassfish.grizzly.http.server.filecache.FileCache fileCache, Throwable error) {
            cacheErrorCount.incrementAndGet();
        }

    } // END JMXFileCacheProbe

}
