/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.jvnet.mimepull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

class FactoryFinder {

    private static ClassLoader cl = FactoryFinder.class.getClassLoader();

    static Object find(String factoryId) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        String systemProp = System.getProperty(factoryId);
        if (systemProp != null) {
            return newInstance(systemProp);
        }

        String providerName = findJarServiceProviderName(factoryId);
        if (providerName != null && providerName.trim().length() > 0) {
            return newInstance(providerName);
        }

        return null;
    }

    static Object newInstance(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        Class providerClass = cl.loadClass(className);
        Object instance = providerClass.newInstance();
        return instance;
    }

    private static String findJarServiceProviderName(String factoryId) {
        String serviceId = "META-INF/services/" + factoryId;
        InputStream is;
        is = cl.getResourceAsStream(serviceId);

        if (is == null) {
            return null;
        }

        String factoryClassName;
        BufferedReader rd = null;
        try {
            rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            try {
                factoryClassName = rd.readLine();
            } catch (IOException x) {
                return null;
            }
        } finally {
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException ex) {
                    Logger.getLogger(FactoryFinder.class.getName()).log(Level.INFO, null, ex);
                }
            }
        }

        return factoryClassName;
    }

}
