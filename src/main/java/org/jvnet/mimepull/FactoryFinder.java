package org.jvnet.mimepull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        InputStream is = null;
        is = cl.getResourceAsStream(serviceId);

        if (is == null) {
            return null;
        }

        BufferedReader rd;
        try {
            rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            rd = new BufferedReader(new InputStreamReader(is));
        }

        String factoryClassName = null;
        try {
            factoryClassName = rd.readLine();
            rd.close();
        } catch (IOException x) {
            return null;
        }

        return factoryClassName;
    }

}
