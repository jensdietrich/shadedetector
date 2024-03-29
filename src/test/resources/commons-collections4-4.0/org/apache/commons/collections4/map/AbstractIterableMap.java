/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.map;

import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.map.EntrySetToMapIteratorAdapter;

/**
 * Provide a basic {@link IterableMap} implementation.
 *
 * @since 4.0
 * @version $Id: AbstractIterableMap.java 1469004 2013-04-17 17:37:03Z tn $
 */
public abstract class AbstractIterableMap<K, V> implements IterableMap<K, V> {

    /**
     * {@inheritDoc}
     */
    public MapIterator<K, V> mapIterator() {
        return new EntrySetToMapIteratorAdapter<K, V>(entrySet());
    }
}
