/*
 * Copyright (c) 2008, 2017 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.glassfish.grizzly.servlet;

/**
 * Constants based on Servlet3.0 spec.
 * This class will be able to be replaced by Servlet3.0 API
 * such as <code>javax.servlet.RequestDispatcher</code> and <code>javax.servlet.DispatcherType</code>
 *
 * @author Bongjae Chang
 */
public class DispatcherConstants {

    static final String FORWARD_REQUEST_URI = "javax.servlet.forward.request_uri";

    static final String FORWARD_CONTEXT_PATH = "javax.servlet.forward.context_path";

    static final String FORWARD_PATH_INFO = "javax.servlet.forward.path_info";

    static final String FORWARD_SERVLET_PATH = "javax.servlet.forward.servlet_path";

    static final String FORWARD_QUERY_STRING = "javax.servlet.forward.query_string";

    static final String INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri";

    static final String INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path";

    static final String INCLUDE_PATH_INFO = "javax.servlet.include.path_info";

    static final String INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path";

    static final String INCLUDE_QUERY_STRING = "javax.servlet.include.query_string";

    static final String ERROR_EXCEPTION = "javax.servlet.error.exception";

    static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

    static final String ERROR_MESSAGE = "javax.servlet.error.message";

    static final String ERROR_REQUEST_URI = "javax.servlet.error.request_uri";

    static final String ERROR_SERVLET_NAME = "javax.servlet.error.servlet_name";

    static final String ERROR_STATUS_CODE = "javax.servlet.error.status_code";

    // async
    static final String ASYNC_REQUEST_URI = "javax.servlet.async.request_uri";

    static final String ASYNC_CONTEXT_PATH = "javax.servlet.async.context_path";

    static final String ASYNC_PATH_INFO = "javax.servlet.async.path_info";

    static final String ASYNC_SERVLET_PATH = "javax.servlet.async.servlet_path";

    static final String ASYNC_QUERY_STRING = "javax.servlet.async.query_string";
}
