/*
 * Copyright (c) 2023, Alibaba Group Holding Limited. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import jdk.internal.loader.Resource;
import jdk.internal.loader.URLClassPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;

public class TestVerificationFailure {
    private static final Path CLASSES_DIR = Paths.get(System.getProperty("test.classes"));

    private static void registerClassLoader(ClassLoader loader, String loaderName) throws Exception {
        try {
            Method m = Class.forName("com.alibaba.util.Utils").getDeclaredMethod("registerClassLoader",
                    ClassLoader.class, String.class);
            m.setAccessible(true);
            m.invoke(null, loader, loaderName);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) throws Exception {
        //  class loader with name
        testURLClassLoader();
    }

    public static byte[] getBytes(String name) {
        try {
            String[] ss = name.split("\\.");
            ss[ss.length-1] += ".class";
            File file = CLASSES_DIR.resolve(ss[0]).resolve(ss[1]).toFile();
            System.out.println(">>> " + name + " " + file.getAbsolutePath());
            InputStream is = new FileInputStream(file);
            byte[] by = new byte[is.available()];
            is.read(by);
            is.close();
            return by;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public static CodeSource getCS(String name, URL[] urls) {
        String path = name.replace('.', '/').concat(".class");
        URLClassPath ucp = new URLClassPath(urls, AccessController.getContext());
        Resource res = ucp.getResource(path, false);
        CodeSigner[] signers = res.getCodeSigners();
        URL url = res.getCodeSourceURL();
        CodeSource cs = new CodeSource(url, signers);
        return cs;
    }

    public static void testURLClassLoader() throws Exception {
        URL[] urls = new URL[] { CLASSES_DIR.toUri().toURL() };

        URLClassLoader loaderParent = new URLClassLoader(urls, ClassLoader.getSystemClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.contains("Parent")) {
                    if (findLoadedClass(name) != null) {
                        return findLoadedClass(name);
                    }
                    byte[] bytes = getBytes(name);
                    return defineClass(name, bytes, 0, bytes.length, getCS(name, urls));
                } else if (name.contains("Child")) {
                    throw new Error("SHOULD_NOT_REACH_HERE");
                } else {
                    return getParent().loadClass(name);
                }
            }
        };
        registerClassLoader(loaderParent, "loaderParent");

        URLClassLoader loaderChildA = new URLClassLoader(urls, loaderParent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.contains("ChildA")) {
                    if (findLoadedClass(name) != null) {
                        return findLoadedClass(name);
                    }
                    byte[] bytes = getBytes(name);
                    return defineClass(name, bytes, 0, bytes.length, getCS(name, urls));
                } else if (name.contains("ChildB")) {
                    throw new Error("SHOULD_NOT_REACH_HERE");
                } else {
                    return getParent().loadClass(name);
                }
            }
        };
        registerClassLoader(loaderChildA, "loaderChildA");

        URLClassLoader loaderChildB = new URLClassLoader(urls, loaderChildA) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.contains("ChildB")) {
                    if (findLoadedClass(name) != null) {
                        return findLoadedClass(name);
                    }
                    byte[] bytes = getBytes(name);
                    return defineClass(name, bytes, 0, bytes.length, getCS(name, urls));
                } else {
                    return getParent().loadClass(name);
                }
            }
        };
        registerClassLoader(loaderChildB, "loaderChildB");


        try {
            Class<?> c = Class.forName("trivial.Parent", true, loaderParent);
            System.out.println(c.getClassLoader());
            Class<?> c1 = Class.forName("trivial.ChildA", true, loaderChildA);
            System.out.println(c1.getClassLoader());
            Class<?> c2 = Class.forName("trivial.ChildB", true, loaderChildB);
            System.out.println(c2.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

