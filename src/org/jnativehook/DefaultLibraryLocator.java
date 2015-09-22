/* JNativeHook: Global keyboard and mouse hooking for Java.
 * Copyright (C) 2006-2015 Alexander Barker.  All Rights Received.
 * https://github.com/kwhat/jnativehook/
 *
 * JNativeHook is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JNativeHook is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jnativehook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Logger;

/**
 * Default implementation of the <code>NativeLibraryLocator</code> interface.  This will first attempt to load the
 * native library from the java.library.path property.  If that fails, it will attempt to extract a library from the
 * jar based on the host operating system and architecture.
 * <p>
 *
 * @author Alexander Barker (<a href="mailto:alex@1stleg.com">alex@1stleg.com</a>)
 * @version 2.0
 * @since 2.0
 * @see NativeLibraryLocator
 */
public class DefaultLibraryLocator implements NativeLibraryLocator {

    private static final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    /**
     * Perform default procedures to interface with the native library. These
     * procedures include unpacking and loading the library into the Java
     * Virtual Machine.
     */
    public Iterator<File> getLibraries() {
        List<File> libraries = new ArrayList<>(1);
        String libName = System.getProperty("jnativehook.lib.name", "JNativeHook");
        String basePackage = GlobalScreen.class.getPackage().getName().replace('.', '/');
        String libNativeName = System.mapLibraryName(libName);
        libNativeName = libNativeName.replaceAll("\\.jnilib$", "\\.dylib");
        StringBuilder libResourcePath = new StringBuilder("/");
        libResourcePath.append(basePackage).append("/lib/");
        libResourcePath.append(NativeSystem.getFamily().toString().toLowerCase()).append('/');
        libResourcePath.append(NativeSystem.getArchitecture().toString().toLowerCase()).append('/');
        libResourcePath.append(libNativeName);
        int i = libNativeName.lastIndexOf('.');
        String libNativePrefix = libNativeName.substring(0, i) + '-';
        String libNativeSuffix = libNativeName.substring(i);
        String libNativeVersion = null;
        InputStream libInputStream = GlobalScreen.class.getResourceAsStream(libResourcePath.toString());
        if (libInputStream != null) {
            try {
                URL jarFile = GlobalScreen.class.getProtectionDomain().getCodeSource().getLocation();
                try (JarInputStream jarInputStream = new JarInputStream(jarFile.openStream())) {
                    Manifest manifest = jarInputStream.getManifest();
                    if (manifest != null) {
                        Attributes attributes = manifest.getAttributes(basePackage);
                        if (attributes != null) {
                            String version = attributes.getValue("Specification-Version");
                            String revision = attributes.getValue("Implementation-Version");
                            libNativeVersion = version + '.' + revision;
                        } else {
                            logger.warning("Invalid library manifest!\n");
                        }
                    } else {
                        logger.warning("Cannot find library manifest!\n");
                    }
                }
            } catch (IOException e) {
                logger.severe(e.getMessage());
            }
            try {
                File libFile;
                if (libNativeVersion != null) {
                    libFile = new File(System.getProperty("java.io.tmpdir"),
                            libNativePrefix + libNativeVersion + libNativeSuffix);
                } else {
                    libFile = File.createTempFile(libNativePrefix, libNativeSuffix);
                }
                byte[] buffer = new byte[4 * 1024];
                int size;
                try (FileOutputStream libOutputStream = new FileOutputStream(libFile)) {
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    DigestInputStream digestInputStream = new DigestInputStream(libInputStream, sha1);
                    while ((size = digestInputStream.read(buffer)) != -1)
                        libOutputStream.write(buffer, 0, size);
                    String sha1Sum = new BigInteger(1, sha1.digest()).toString(16).toUpperCase();
                    if (libNativeVersion == null) {
                        libNativeVersion = sha1Sum;
                        File newFile = new File(System.getProperty("java.io.tmpdir"),
                                libNativePrefix + libNativeVersion + libNativeSuffix);
                        if (libFile.renameTo(newFile))
                            libFile = newFile;
                    }
                    System.setProperty("jnativehook.lib.version", libNativeVersion);
                    libraries.add(libFile);
                    logger.info("Library extracted successfully: " + libFile.getPath() + " (0x" + sha1Sum + ").\n");
                }
            } catch (IOException | NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        } else {
            logger.severe("Unable to extract the native library " + libResourcePath.toString() + "!\n");
        }
        return libraries.iterator();
    }
}
