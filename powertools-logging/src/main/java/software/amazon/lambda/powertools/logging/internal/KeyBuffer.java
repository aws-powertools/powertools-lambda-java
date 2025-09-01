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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Generic buffer data structure for storing events by key with size-based eviction.
 * 
 * <p>This buffer maintains separate event queues for each key, with configurable size limits
 * to prevent memory exhaustion. When buffers exceed their size limit, older events are
 * automatically evicted to make room for newer ones.
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Per-key buffering:</strong> Each key maintains its own independent buffer</li>
 *   <li><strong>Size-based eviction:</strong> Oldest events are removed when buffer size exceeds limit</li>
 *   <li><strong>Overflow protection:</strong> Events larger than buffer size are rejected entirely</li>
 *   <li><strong>Thread-safe:</strong> Supports concurrent access across different keys</li>
 *   <li><strong>Overflow tracking:</strong> Logs warnings when events are evicted or rejected</li>
 * </ul>
 * 
 * <h3>Eviction Behavior:</h3>
 * <ul>
 *   <li><strong>Buffer overflow:</strong> When adding an event would exceed maxBytes, oldest events are evicted first</li>
 *   <li><strong>Large events:</strong> Events larger than maxBytes are rejected without evicting existing events</li>
 *   <li><strong>FIFO eviction:</strong> Events are removed in first-in-first-out order during overflow</li>
 *   <li><strong>Overflow warnings:</strong> Automatic logging when events are evicted or rejected</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This class is thread-safe for concurrent operations. Different keys can be accessed
 * simultaneously, and operations on the same key are synchronized to prevent data corruption.
 * 
 * @param <K> the type of key used for buffering (e.g., String for trace IDs)
 * @param <T> the type of events to buffer (must be compatible with the size calculator)
 */
public class KeyBuffer<K, T> {

    private final Map<K, Deque<T>> keyBufferCache = new ConcurrentHashMap<>();
    private final Map<K, Boolean> overflowTriggered = new ConcurrentHashMap<>();
    private final int maxBytes;
    private final Function<T, Integer> sizeCalculator;
    private final Runnable overflowWarningLogger;

    @SuppressWarnings("java:S106") // Using System.err to avoid circular dependency with logging implementation
    public KeyBuffer(int maxBytes, Function<T, Integer> sizeCalculator) {
        this(maxBytes, sizeCalculator, () -> System.err.println("WARN [" + KeyBuffer.class.getSimpleName()
                + "] - Some logs are not displayed because they were evicted from the buffer. Increase buffer size to store more logs in the buffer."));
    }

    public KeyBuffer(int maxBytes, Function<T, Integer> sizeCalculator, Runnable overflowWarningLogger) {
        this.maxBytes = maxBytes;
        this.sizeCalculator = sizeCalculator;
        this.overflowWarningLogger = overflowWarningLogger;
    }

    public void add(K key, T event) {
        int eventSize = sizeCalculator.apply(event);
        if (eventSize > maxBytes) {
            overflowTriggered.put(key, true);
            return;
        }

        Deque<T> buffer = keyBufferCache.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (buffer) {
            buffer.add(event);
            while (getBufferSize(buffer) > maxBytes && !buffer.isEmpty()) {
                overflowTriggered.put(key, true);
                buffer.removeFirst();
            }
        }
    }

    public Deque<T> removeAll(K key) {
        logOverflowWarningIfNeeded(key);
        Deque<T> buffer = keyBufferCache.remove(key);
        if (buffer != null) {
            synchronized (buffer) {
                return new ArrayDeque<>(buffer);
            }
        }
        return buffer;
    }

    public void clear(K key) {
        keyBufferCache.remove(key);
        overflowTriggered.remove(key);
    }

    private void logOverflowWarningIfNeeded(K key) {
        if (Boolean.TRUE.equals(overflowTriggered.remove(key))) {
            overflowWarningLogger.run();
        }
    }

    private int getBufferSize(Deque<T> buffer) {
        return buffer.stream().mapToInt(sizeCalculator::apply).sum();
    }
}
