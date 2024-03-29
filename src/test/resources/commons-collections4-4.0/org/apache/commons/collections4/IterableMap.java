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
package org.apache.commons.collections4;

import org.apache.commons.collections4.IterableGet;
import org.apache.commons.collections4.Put;

import java.util.Map;

/**
 * Defines a map that can be iterated directly without needing to create an entry set.
 * <p>
 * A map iterator is an efficient way of iterating over maps.
 * There is no need to access the entry set or use Map Entry objects.
 * <pre>
 * IterableMap<String,Integer> map = new HashedMap<String,Integer>();
 * MapIterator<String,Integer> it = map.mapIterator();
 * while (it.hasNext()) {
 *   String key = it.next();
 *   Integer value = it.getValue();
 *   it.setValue(value + 1);
 * }
 * </pre>
 *
 * @param <K> the type of the keys in the map
 * @param <V> the type of the values in the map
 *
 * @since 3.0
 * @version $Id: IterableMap.java 1469004 2013-04-17 17:37:03Z tn $
 */
public interface IterableMap<K, V> extends Map<K, V>, Put<K, V>, IterableGet<K, V> {
}
