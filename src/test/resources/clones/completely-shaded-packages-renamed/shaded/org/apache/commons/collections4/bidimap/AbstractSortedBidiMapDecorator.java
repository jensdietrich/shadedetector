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
package shaded.org.apache.commons.collections4.bidimap;

import shaded.org.apache.commons.collections4.SortedBidiMap;

import java.util.Comparator;
import java.util.SortedMap;

/**
 * Provides a base decorator that enables additional functionality to be added
 * to a SortedBidiMap via decoration.
 * <p>
 * Methods are forwarded directly to the decorated map.
 * <p>
 * This implementation does not perform any special processing with the map views.
 * Instead it simply returns the inverse from the wrapped map. This may be
 * undesirable, for example if you are trying to write a validating implementation
 * it would provide a loophole around the validation.
 * But, you might want that loophole, so this class is kept simple.
 *
 * @since 3.0
 * @version $Id: AbstractSortedBidiMapDecorator.java 1477745 2013-04-30 18:08:32Z tn $
 */
public abstract class AbstractSortedBidiMapDecorator<K, V>
        extends AbstractOrderedBidiMapDecorator<K, V> implements SortedBidiMap<K, V> {

    /**
     * Constructor that wraps (not copies).
     *
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if the collection is null
     */
    public AbstractSortedBidiMapDecorator(final SortedBidiMap<K, V> map) {
        super(map);
    }

    /**
     * Gets the map being decorated.
     *
     * @return the decorated map
     */
    @Override
    protected SortedBidiMap<K, V> decorated() {
        return (SortedBidiMap<K, V>) super.decorated();
    }

    //-----------------------------------------------------------------------
    @Override
    public SortedBidiMap<V, K> inverseBidiMap() {
        return decorated().inverseBidiMap();
    }

    public Comparator<? super K> comparator() {
        return decorated().comparator();
    }

    public Comparator<? super V> valueComparator() {
        return decorated().valueComparator();
    }

    public SortedMap<K, V> subMap(final K fromKey, final K toKey) {
        return decorated().subMap(fromKey, toKey);
    }

    public SortedMap<K, V> headMap(final K toKey) {
        return decorated().headMap(toKey);
    }

    public SortedMap<K, V> tailMap(final K fromKey) {
        return decorated().tailMap(fromKey);
    }

}
