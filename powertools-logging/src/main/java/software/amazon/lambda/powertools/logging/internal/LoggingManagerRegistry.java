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

package software.amazon.lambda.powertools.logging.internal;

import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread-safe singleton registry for LoggingManager instances.
 * Handles lazy loading and caching of the LoggingManager implementation.
 */
public final class LoggingManagerRegistry {

    private static final AtomicReference<LoggingManager> instance = new AtomicReference<>();

    private LoggingManagerRegistry() {
        // Utility class
    }

    /**
     * Gets the LoggingManager instance, loading it lazily on first access.
     *
     * @return the LoggingManager instance
     */
    public static LoggingManager getLoggingManager() {
        LoggingManager manager = instance.get();
        if (manager == null) {
            synchronized (LoggingManagerRegistry.class) {
                manager = instance.get();
                if (manager == null) {
                    manager = loadLoggingManager();
                    instance.set(manager);
                }
            }
        }
        return manager;
    }

    @SuppressWarnings("java:S106") // S106: System.err is used rather than logger to make sure message is printed
    private static LoggingManager loadLoggingManager() {
        ServiceLoader<LoggingManager> loggingManagers;
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager == null) {
            loggingManagers = ServiceLoader.load(LoggingManager.class);
        } else {
            final PrivilegedAction<ServiceLoader<LoggingManager>> action = () -> ServiceLoader
                    .load(LoggingManager.class);
            loggingManagers = AccessController.doPrivileged(action);
        }

        List<LoggingManager> loggingManagerList = new ArrayList<>();
        for (LoggingManager lm : loggingManagers) {
            loggingManagerList.add(lm);
        }
        return selectLoggingManager(loggingManagerList, System.err);
    }

    static LoggingManager selectLoggingManager(List<LoggingManager> loggingManagerList, PrintStream printStream) {
        LoggingManager loggingManager;
        if (loggingManagerList.isEmpty()) {
            printStream.println("ERROR. No LoggingManager was found on the classpath");
            printStream.println("ERROR. Applying default LoggingManager: POWERTOOLS_LOG_LEVEL variable is ignored");
            printStream.println(
                    "ERROR. Make sure to add either powertools-logging-log4j or powertools-logging-logback to your dependencies");
            loggingManager = new DefaultLoggingManager();
        } else {
            if (loggingManagerList.size() > 1) {
                printStream.println("WARN. Multiple LoggingManagers were found on the classpath");
                for (LoggingManager manager : loggingManagerList) {
                    printStream.println("WARN. Found LoggingManager: [" + manager + "]");
                }
                printStream.println(
                        "WARN. Make sure to have only one of powertools-logging-log4j OR powertools-logging-logback to your dependencies");
                printStream.println("WARN. Using the first LoggingManager found on the classpath: ["
                        + loggingManagerList.get(0) + "]");
            }
            loggingManager = loggingManagerList.get(0);
        }
        return loggingManager;
    }
}
