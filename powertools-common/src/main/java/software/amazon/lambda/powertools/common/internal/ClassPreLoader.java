/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package software.amazon.lambda.powertools.common.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to preload classes to support automatic priming for SnapStart
 */
public final class ClassPreLoader {
    public static final String CLASSES_FILE = "classesloaded.txt";

    private static final Logger LOG = LoggerFactory.getLogger(ClassPreLoader.class);

    // A binary class name contains only letters, digits, and the '.', '_' and '$' characters. This
    // filters out malformed entries such as runtime-synthetic lambda classes
    // (e.g. "com.example.Foo$$Lambda$1/0x0000...") and lines that contain a path or other junk
    // (e.g. a URL-encoded space "%20" followed by a file path), none of which Class.forName can
    // load. The character class is a single linear match, so it is not prone to backtracking.
    private static final Pattern BINARY_CLASS_NAME = Pattern.compile("[\\p{L}\\p{N}_$.]+");

    private ClassPreLoader() {
        // Hide default constructor
    }

    /**
     * Initializes the classes listed in the classesloaded resource
     */
    public static void preloadClasses() {
        try {
            Enumeration<URL> files = ClassPreLoader.class.getClassLoader().getResources(CLASSES_FILE);
            // If there are multiple files, preload classes from all of them
            while (files.hasMoreElements()) {
                URL url = files.nextElement();
                URLConnection conn = url.openConnection();
                conn.setUseCaches(false);
                InputStream is = conn.getInputStream();
                preloadClassesFromStream(is);
            }
        } catch (IOException ignored) {
            // No action is required if preloading fails for any reason
        }
    }

    /**
     * Loads the list of classes passed as a stream
     *
     * @param is
     */
    private static void preloadClassesFromStream(InputStream is) {
        int loaded = 0;
        try (is;
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('#');
                if (idx != -1) {
                    line = line.substring(0, idx);
                }
                final String className = line.strip();
                if (!className.isBlank() && BINARY_CLASS_NAME.matcher(className).matches()
                        && loadClassIfFound(className)) {
                    loaded++;
                }
            }
        } catch (Exception ignored) {
            // No action is required if preloading fails for any reason
        }
        LOG.debug("SnapStart priming: preloaded {} class(es) from {}", loaded, CLASSES_FILE);
    }

    /**
     * Initializes the class with given name if found, ignores otherwise
     *
     * @param className the binary name of the class to load
     * @return true if the class was found and loaded, false otherwise
     */
    private static boolean loadClassIfFound(String className) {
        try {
            Class.forName(className, true, ClassPreLoader.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            // No action is required if the class with given name cannot be found
            return false;
        }
    }
}