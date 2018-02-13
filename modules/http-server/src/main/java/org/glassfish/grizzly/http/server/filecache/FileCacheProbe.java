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

/**
 * Monitoring probe providing callbacks that may be invoked by Grizzly {@link FileCache}.
 *
 * @author Alexey Stashok
 *
 * @since 2.0
 */
public interface FileCacheProbe {
    /**
     * Method will be called, when file cache entry gets added.
     *
     * @param fileCache {@link FileCache}, the event belongs to.
     * @param entry {@link FileCacheEntry} been added.
     */
    void onEntryAddedEvent(FileCache fileCache, FileCacheEntry entry);

    /**
     * Method will be called, when file cache entry gets removed.
     *
     * @param fileCache {@link FileCache}, the event belongs to.
     * @param entry {@link FileCacheEntry} been removed.
     */
    void onEntryRemovedEvent(FileCache fileCache, FileCacheEntry entry);

    /**
     * Method will be called, when file cache entry gets hit.
     *
     * @param fileCache {@link FileCache}, the event belongs to.
     * @param entry {@link FileCacheEntry} been hitted.
     */
    void onEntryHitEvent(FileCache fileCache, FileCacheEntry entry);

    /**
     * Method will be called, when file cache entry is missed for some resource.
     *
     * @param fileCache {@link FileCache}, the event belongs to.
     * @param host the requested HTTP "Host" header.
     * @param requestURI the requested HTTP URL.
     */
    void onEntryMissedEvent(FileCache fileCache, String host, String requestURI);

    /**
     * Method will be called, when error occurs on the {@link FileCache}.
     *
     * @param fileCache {@link FileCache}, the event belongs to.
     * @param error error
     */
    void onErrorEvent(FileCache fileCache, Throwable error);


    // ---------------------------------------------------------- Nested Classes

    /**
     * {@link FileCacheProbe} adapter that provides no-op implementations for
     * all interface methods allowing easy extension by the developer.
     *
     * @since 2.1.9
     */
    @SuppressWarnings("UnusedDeclaration")
    class Adapter implements FileCacheProbe {


        // ----------------------------------------- Methods from FileCacheProbe

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEntryAddedEvent(FileCache fileCache, FileCacheEntry entry) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEntryRemovedEvent(FileCache fileCache, FileCacheEntry entry) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEntryHitEvent(FileCache fileCache, FileCacheEntry entry) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onEntryMissedEvent(FileCache fileCache, String host, String requestURI) {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void onErrorEvent(FileCache fileCache, Throwable error) {}

    } // END Adapter
}
