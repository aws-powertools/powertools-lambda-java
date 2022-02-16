/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.
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
package software.amazon.lambda.powertools.idempotency.internal.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of a simple LRU Cache based on a {@link LinkedHashMap}
 * See <a href="https://stackoverflow.com/a/6400874/270653">here</a>.
 * @param <K> Type of the keys
 * @param <V> Types of the values
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 3108262622672699228L;
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity * 4 / 3, 0.75f, true);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry entry) {
        return (size() > this.capacity);
    }
}
