/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper utility to support jdk <= jdk1.6. After jdk1.6 EOL reflection can be removed and API can be used directly.
 */
class TempFiles {

    private static final Logger LOGGER = Logger.getLogger(TempFiles.class.getName());

    private static final Class<?> CLASS_FILES;
    private static final Class<?> CLASS_PATH;
    private static final Class<?> CLASS_FILE_ATTRIBUTE;
    private static final Class<?> CLASS_FILE_ATTRIBUTES;
    private static final Method METHOD_FILE_TO_PATH;
    private static final Method METHOD_FILES_CREATE_TEMP_FILE;
    private static final Method METHOD_FILES_CREATE_TEMP_FILE_WITHPATH;

    private static final Method METHOD_PATH_TO_FILE;

    private static boolean useJdk6API;

    static {
        useJdk6API = isJdk6();

        CLASS_FILES = safeGetClass("java.nio.file.Files");
        CLASS_PATH = safeGetClass("java.nio.file.Path");
        CLASS_FILE_ATTRIBUTE = safeGetClass("java.nio.file.attribute.FileAttribute");
        CLASS_FILE_ATTRIBUTES = safeGetClass("[Ljava.nio.file.attribute.FileAttribute;");
        METHOD_FILE_TO_PATH = safeGetMethod(File.class, "toPath");
        METHOD_FILES_CREATE_TEMP_FILE = safeGetMethod(CLASS_FILES, "createTempFile", String.class, String.class, CLASS_FILE_ATTRIBUTES);
        METHOD_FILES_CREATE_TEMP_FILE_WITHPATH = safeGetMethod(CLASS_FILES, "createTempFile", CLASS_PATH, String.class, String.class, CLASS_FILE_ATTRIBUTES);
        METHOD_PATH_TO_FILE = safeGetMethod(CLASS_PATH, "toFile");
    }

    private static boolean isJdk6() {
        String javaVersion = System.getProperty("java.version");
        LOGGER.log(Level.FINEST, "Detected java version = {0}", javaVersion);
        return javaVersion.startsWith("1.6.");
    }

    private static Class<?> safeGetClass(String className) {
        // it is jdk 6 or something failed already before
        if (useJdk6API) return null;
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Exception cought", e);
            LOGGER.log(Level.WARNING, "Class {0} not found. Temp files will be created using old java.io API.", className);
            useJdk6API = true;
            return null;
        }
    }

    private static Method safeGetMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        // it is jdk 6 or something failed already before
        if (useJdk6API) return null;
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            LOGGER.log(Level.SEVERE, "Exception cought", e);
            LOGGER.log(Level.WARNING, "Method {0} not found. Temp files will be created using old java.io API.", methodName);
            useJdk6API = true;
            return null;
        }
    }


    static Object toPath(File f) throws InvocationTargetException, IllegalAccessException {
        return METHOD_FILE_TO_PATH.invoke(f);
    }

    static File toFile(Object path) throws InvocationTargetException, IllegalAccessException {
        return (File) METHOD_PATH_TO_FILE.invoke(path);
    }

    static File createTempFile(String prefix, String suffix, File dir) throws IOException {

        if (useJdk6API) {
            LOGGER.log(Level.FINEST, "Jdk6 detected, temp file (prefix:{0}, suffix:{1}) being created using old java.io API.", new Object[]{prefix, suffix});
            return File.createTempFile(prefix, suffix, dir);

        } else {

            try {
                if (dir != null) {
                    Object path = toPath(dir);
                    LOGGER.log(Level.FINEST, "Temp file (path: {0}, prefix:{1}, suffix:{2}) being created using NIO API.", new Object[]{dir.getAbsolutePath(), prefix, suffix});
                    return toFile(METHOD_FILES_CREATE_TEMP_FILE_WITHPATH.invoke(null, path, prefix, suffix, Array.newInstance(CLASS_FILE_ATTRIBUTE, 0)));
                } else {
                    LOGGER.log(Level.FINEST, "Temp file (prefix:{0}, suffix:{1}) being created using NIO API.", new Object[]{prefix, suffix});
                    return toFile(METHOD_FILES_CREATE_TEMP_FILE.invoke(null, prefix, suffix, Array.newInstance(CLASS_FILE_ATTRIBUTE, 0)));
                }

            } catch (IllegalAccessException e) {
                LOGGER.log(Level.SEVERE, "Exception caught", e);
                LOGGER.log(Level.WARNING, "Error invoking java.nio API, temp file (path: {0}, prefix:{1}, suffix:{2}) being created using old java.io API.",
                        new Object[]{dir != null ? dir.getAbsolutePath() : null, prefix, suffix});
                return File.createTempFile(prefix, suffix, dir);

            } catch (InvocationTargetException e) {
                LOGGER.log(Level.SEVERE, "Exception caught", e);
                LOGGER.log(Level.WARNING, "Error invoking java.nio API, temp file (path: {0}, prefix:{1}, suffix:{2}) being created using old java.io API.",
                        new Object[]{dir != null ? dir.getAbsolutePath() : null, prefix, suffix});
                return File.createTempFile(prefix, suffix, dir);
            }
        }

    }


}