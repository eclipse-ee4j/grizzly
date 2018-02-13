/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.grizzly.http.server.util.HtmlHelper;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.grizzly.http.util.HttpUtils;

/**
 * Default Grizzly {@link ErrorPageGenerator}.
 */
public class DefaultErrorPageGenerator implements ErrorPageGenerator {

    /**
     * {@inheritDoc}
     */
    @Override
    public String generate(final Request request,
            final int status, final String reasonPhrase,
            final String description, final Throwable exception) {
        if (status == 404) {
            return HtmlHelper.getErrorPage(HttpStatus.NOT_FOUND_404.getReasonPhrase(),
                    "Resource identified by path '" +
                            HttpUtils.filter(request.getRequestURI()) +
                            "', does not exist.",
                    request.getServerFilter().getFullServerName());
        }

        return HtmlHelper.getExceptionErrorPage(reasonPhrase, description,
                request.getServerFilter().getFullServerName(),
                exception);
    }
}
