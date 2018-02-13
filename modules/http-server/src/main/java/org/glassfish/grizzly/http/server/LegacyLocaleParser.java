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

import java.util.Locale;

class LegacyLocaleParser implements LocaleParser {


    // ------------------------------------------------ Methods from LocalParser


    @Override
    public Locale parseLocale(String source) {
        // Extract the language and country for this source
        String language;
        String country;
        String variant;
        int dash = source.indexOf('-');
        if (dash < 0) {
            language = source;
            country = "";
            variant = "";
        } else {
            language = source.substring(0, dash);
            country = source.substring(dash + 1);
            int vDash = country.indexOf('-');
            if (vDash > 0) {
                String cTemp = country.substring(0, vDash);
                variant = country.substring(vDash + 1);
                country = cTemp;
            } else {
                variant = "";
            }
        }

        if (!isAlpha(language) || !isAlpha(country) || !isAlpha(variant)) {
            return null;
        }

        return new Locale(language, country, variant);
    }


    // --------------------------------------------------------- Private Methods


    /*
     * @return <code>true</code> if the given string is composed of
     *  upper- or lowercase letters only, <code>false</code> otherwise.
     */
    private static boolean isAlpha(String value) {

        if (value == null) {
            return false;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }

        return true;
    }

}
