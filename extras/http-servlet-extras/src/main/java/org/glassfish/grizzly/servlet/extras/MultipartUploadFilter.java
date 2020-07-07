/*
 * Copyright (c) 2011, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.servlet.extras;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.EmptyCompletionHandler;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOInputStream;
import org.glassfish.grizzly.http.io.NIOReader;
import org.glassfish.grizzly.http.multipart.ContentDisposition;
import org.glassfish.grizzly.http.multipart.MultipartEntry;
import org.glassfish.grizzly.http.multipart.MultipartEntryHandler;
import org.glassfish.grizzly.http.multipart.MultipartScanner;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.Parameters;
import org.glassfish.grizzly.memory.ByteBufferArray;
import org.glassfish.grizzly.servlet.HttpServletRequestImpl;
import org.glassfish.grizzly.servlet.HttpServletResponseImpl;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * <p>
 * A {@link Filter} implementation that leverages the non-blocking multipart processing API.
 * </p>
 *
 * <p>
 * NOTE: this filter is implementation specific and will not function properly outside of the Grizzly 2.x Servlet
 * implementation.
 * </p>
 *
 * @since 2.2
 */
public class MultipartUploadFilter implements Filter {

    /**
     * Filter initialization parameter name to control whether or not the temp files used to store the uploaded file bytes
     * will be deleted after the request ends. If this parameter is omitted, it will be assumed that the files will be
     * deleted after request end.
     */
    public static final String DELETE_ON_REQUEST_END = "org.glassfish.grizzly.multipart.DELETE_ON_REQUEST_END";

    /**
     * The name of the request attribute with which an array of all Files (java.io.File[]) that were uploaded will be
     * stored.
     */
    public static final java.lang.String UPLOADED_FILES = "org.glassfish.grizzly.multipart.UPLOADED_FILES";

    private static final Logger LOGGER = Grizzly.logger(MultipartUploadFilter.class);
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private static final String MULTIPART_FORM = "multipart/form-data";

    private boolean deleteAfterRequestEnd = true;

    private File tempDir;

    // ----------------------------------------------------- Methods from Filter

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        final String delete = filterConfig.getInitParameter(DELETE_ON_REQUEST_END);
        if (delete != null) {
            deleteAfterRequestEnd = Boolean.getBoolean(delete);
        }

        File baseTempDir = new File(System.getProperty(JAVA_IO_TMPDIR));
        tempDir = new File(baseTempDir, Long.toString(System.currentTimeMillis()));
        tempDir.deleteOnExit();

    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {

        final String contentType = servletRequest.getContentType();
        if (contentType == null || !contentType.startsWith(MULTIPART_FORM)) {
            filterChain.doFilter(servletRequest, servletResponse);
        }
        validateRequestResponse(servletRequest, servletResponse);
        final String dirName = Long.toString(System.nanoTime());
        final File dir = new File(tempDir, dirName);
        final Request request = ((HttpServletRequestImpl) servletRequest).getRequest();
        final Response response = ((HttpServletResponseImpl) servletResponse).getResponse();
        response.suspend();
        // Start the asynchronous multipart request scanning...
        final List<File> uploadedFiles = new ArrayList<>();
        final Parameters formParams = new Parameters();
        final UploadMultipartHandler uploadHandler = new UploadMultipartHandler(uploadedFiles, dir, formParams);
        MultipartScanner.scan(request, uploadHandler, new EmptyCompletionHandler<Request>() {
            // CompletionHandler is called once HTTP request processing is completed
            // or failed.
            @Override
            public void completed(final Request request) {
                // Resume the asynchronous HTTP request processing
                // (in other words finish the asynchronous HTTP request processing).
                request.setRequestParameters(formParams);
                servletRequest.setAttribute(UPLOADED_FILES, uploadedFiles.toArray(new File[uploadedFiles.size()]));
                try {
                    filterChain.doFilter(servletRequest, servletResponse);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                } finally {
                    if (deleteAfterRequestEnd) {
                        clean(dir);
                    }
                    response.resume();
                }
            }

            @Override
            public void failed(Throwable throwable) {
                // if failed - log the error
                LOGGER.log(Level.SEVERE, "Upload failed.", throwable);
                // Complete the asynchronous HTTP request processing.
                response.resume();
            }
        });

    }

    @Override
    public void destroy() {

        clean(tempDir);

    }

    // --------------------------------------------------------- Private Methods

    private static void validateRequestResponse(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException {

        if (!(servletRequest instanceof HttpServletRequestImpl)) {
            throw new ServletException("ServletRequest instances passed to MultipartUploadFilter must not be wrapped.");
        }
        if (!(servletResponse instanceof HttpServletResponseImpl)) {
            throw new ServletException("ServletResponse instances passed to MultipartUploadFilter must not be wrapped.");
        }

    }

    private static void clean(final File file) {

        if (file == null || !file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] f = file.listFiles();
            if (f.length == 0) {
                if (!file.delete()) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(String.format("Unable to delete directory %s.  Will attempt deletion again upon JVM exit.", file.getAbsolutePath()));
                    }
                }
            } else {
                for (final File ff : f) {
                    clean(ff);
                }
            }
        } else {
            if (!file.delete()) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(String.format("Unable to delete file %s.  Will attempt deletion again upon JVM exit.", file.getAbsolutePath()));
                }
                file.deleteOnExit();
            }
        }

    }

    // ---------------------------------------------------------- Nested Classes

    /**
     * {@link org.glassfish.grizzly.http.multipart.MultipartEntryHandler}, responsible for processing the upload.
     */
    private static final class UploadMultipartHandler implements MultipartEntryHandler {

        // number of bytes uploaded
        private final AtomicInteger uploadedBytesCounter = new AtomicInteger();
        private final File uploadDir;
        private final List<File> uploadedFiles;
        private final Parameters formParams;

        public UploadMultipartHandler(final List<File> uploadedFiles, final File uploadDir, final Parameters formParams) {
            this.uploadedFiles = uploadedFiles;
            this.uploadDir = uploadDir;
            this.formParams = formParams;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void handle(final MultipartEntry multipartEntry) throws Exception {
            // get the entry's Content-Disposition
            final ContentDisposition contentDisposition = multipartEntry.getContentDisposition();

            final String paramName = contentDisposition.getDispositionParamUnquoted("name");
            // get the filename for Content-Disposition
            final String filename = contentDisposition.getDispositionParamUnquoted("filename");

            if (filename != null && filename.length() > 0) {
                formParams.addParameterValues(paramName, new String[] { filename });
                // Get the NIOInputStream to read the multipart entry content
                final NIOInputStream inputStream = multipartEntry.getNIOInputStream();

                LOGGER.log(Level.FINE, "Uploading file {0}", new Object[] { filename });

                // start asynchronous non-blocking content read.
                inputStream.notifyAvailable(new UploadReadHandler(uploadedFiles, uploadDir, filename, inputStream, uploadedBytesCounter));
            } else {
                // standard param value
                final NIOReader nioReader = multipartEntry.getNIOReader();
                nioReader.notifyAvailable(new ReadHandler() {
                    @Override
                    public void onDataAvailable() throws Exception {
                        // ignored
                    }

                    @Override
                    public void onError(Throwable t) {
                        try {
                            nioReader.close();
                        } catch (IOException ignored) {
                        }
                    }

                    @Override
                    public void onAllDataRead() throws Exception {
                        final char[] chars = new char[nioReader.readyData()];
                        nioReader.read(chars);
                        formParams.addParameterValues(paramName, new String[] { new String(chars) });
                    }
                });
            }
        }

    } // END UploadMultipartHandler

    /**
     * Simple {@link org.glassfish.grizzly.ReadHandler} implementation, which is reading HTTP request content (uploading
     * file) in non-blocking mode and saves the content into the specific file.
     */
    private static class UploadReadHandler implements ReadHandler {

        // Non-blocking multipart entry input stream
        private final NIOInputStream inputStream;

        // the destination file channel, where we save the data.
        private final FileChannel fileOutput;

        private final String filename;

        // uploaded bytes counter
        private final AtomicInteger uploadedBytesCounter;

        private final File uploadFile;

        private final List<File> uploadedFiles;

        private UploadReadHandler(final List<File> uploadedFiles, final File uploadDir, final String filename, final NIOInputStream inputStream,
                final AtomicInteger uploadedBytesCounter) throws FileNotFoundException {

            this.uploadedFiles = uploadedFiles;
            this.filename = filename;
            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                throw new RuntimeException(String.format("Unable to create directory %s", uploadDir.toString()));
            }
            uploadFile = new File(uploadDir, filename);
            fileOutput = new FileOutputStream(uploadFile).getChannel();
            this.inputStream = inputStream;
            this.uploadedBytesCounter = uploadedBytesCounter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onDataAvailable() throws Exception {
            // save available file content
            readAndSaveAvail();

            // register this handler to be notified next time some data
            // becomes available
            inputStream.notifyAvailable(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAllDataRead() throws Exception {
            // save available file content
            readAndSaveAvail();
            // finish the upload
            finish();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onError(Throwable t) {
            LOGGER.log(Level.WARNING, String.format("Upload of file %s failed.", filename), t);
            // finish the upload
            finish();
        }

        /**
         * Read available file content data out of HTTP request and save the chunk into local file output stream
         *
         * @throws IOException
         */
        private void readAndSaveAvail() throws IOException {
            while (inputStream.isReady()) {
                final Buffer buffer = inputStream.readBuffer();
                if (buffer.isComposite()) {
                    writeCompositeBuffer(buffer);
                } else {
                    writeBufferToDiskAndUpdateStats(buffer.toByteBuffer());
                }
            }
        }

        private void writeCompositeBuffer(final Buffer b) throws IOException {
            // Use toByteBufferArray() as the buffer returned by
            // readBuffer() may be a CompositeBuffer - this avoids
            // an unnecessary copy.
            final ByteBufferArray bufferArray = b.toByteBufferArray();
            // Obtain the underlying array, but we still need
            // bufferArray to tell us the number of elements to expect -
            // we can't rely on the length of the array itself.
            final ByteBuffer[] buffers = bufferArray.getArray();
            for (int i = 0, len = bufferArray.size(); i < len; i++) {
                writeBufferToDiskAndUpdateStats(buffers[i]);
            }
        }

        private void writeBufferToDiskAndUpdateStats(final ByteBuffer b) throws IOException {
            uploadedBytesCounter.addAndGet(b.remaining());
            fileOutput.write(b);
        }

        /**
         * Finish the file upload
         */
        private void finish() {
            try {
                // close file output stream
                fileOutput.close();
                uploadedFiles.add(uploadFile);
            } catch (IOException ignored) {
            }
        }
    } // END UploadReadHandler
}
