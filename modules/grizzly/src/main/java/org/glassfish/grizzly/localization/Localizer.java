/*
 * Copyright (c) 2010, 2020 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.localization;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Localizes the {@link Localizable} into a message by using a configured {@link Locale}.
 *
 * @author WS Development Team
 */
public class Localizer {

    private final Locale _locale;
    private final Map<String, ResourceBundle> _resourceBundles;

    public Localizer() {
        this(Locale.getDefault());
    }

    public Localizer(Locale l) {
        _locale = l;
        _resourceBundles = new HashMap<>();
    }

    public Locale getLocale() {
        return _locale;
    }

    public String localize(Localizable l) {
        String key = l.getKey();
        if (key == Localizable.NOT_LOCALIZABLE) {
            // this message is not localizable
            return (String) l.getArguments()[0];
        }
        String bundlename = l.getResourceBundleName();

        try {
            ResourceBundle bundle = _resourceBundles.get(bundlename);

            if (bundle == null) {
                try {
                    bundle = ResourceBundle.getBundle(bundlename, _locale);
                } catch (MissingResourceException e) {
                    // work around a bug in the com.sun.enterprise.deployment.WebBundleArchivist:
                    // all files with an extension different from .class (hence all the .properties files)
                    // get copied to the top level directory instead of being in the package where they
                    // are defined
                    // so, since we can't find the bundle under its proper name, we look for it under
                    // the top-level package

                    int i = bundlename.lastIndexOf('.');
                    if (i != -1) {
                        String alternateBundleName = bundlename.substring(i + 1);
                        try {
                            bundle = ResourceBundle.getBundle(alternateBundleName, _locale);
                        } catch (MissingResourceException e2) {
                            // give up
                            return getDefaultMessage(l);
                        }
                    }
                }

                _resourceBundles.put(bundlename, bundle);
            }

            if (bundle == null) {
                return getDefaultMessage(l);
            }

            if (key == null) {
                key = "undefined";
            }

            String msg;
            try {
                msg = bundle.getString(key);
            } catch (MissingResourceException e) {
                // notice that this may throw a MissingResourceException of its own (caught below)
                msg = bundle.getString("undefined");
            }

            // localize all arguments to the given localizable object
            Object[] args = l.getArguments();
            for (int i = 0; i < args.length; ++i) {
                if (args[i] instanceof Localizable) {
                    args[i] = localize((Localizable) args[i]);
                }
            }

            return MessageFormat.format(msg, args);

        } catch (MissingResourceException e) {
            return getDefaultMessage(l);
        }

    }

    private String getDefaultMessage(Localizable l) {
        String key = l.getKey();
        Object[] args = l.getArguments();
        StringBuilder sb = new StringBuilder();
        sb.append("[failed to localize] ");
        sb.append(key);
        if (args != null) {
            sb.append('(');
            for (int i = 0; i < args.length; ++i) {
                if (i != 0) {
                    sb.append(", ");
                }
                sb.append(String.valueOf(args[i]));
            }
            sb.append(')');
        }
        return sb.toString();
    }

}
