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

/**
 * Used to preload classes to support automatic priming for SnapStart
 */
public class ClassPreLoader {
    public static final String CLASSES_FILE = "classesloaded.txt";

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
        try (is;
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('#');
                if (idx != -1) {
                    line = line.substring(0, idx);
                }
                final String className = line.stripTrailing();
                if (!className.isBlank()) {
                    Class.forName(className, true, ClassPreLoader.class.getClassLoader());
                }
            }
        } catch (Exception ignored) {
            // No action is required if preloading fails for any reason
        }
    }
}