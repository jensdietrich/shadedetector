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

import shaded.org.apache.commons.collections4.OrderedBidiMap;
import shaded.org.apache.commons.collections4.OrderedMapIterator;
import shaded.org.apache.commons.collections4.Unmodifiable;
import shaded.org.apache.commons.collections4.iterators.UnmodifiableOrderedMapIterator;
import shaded.org.apache.commons.collections4.map.UnmodifiableEntrySet;
import shaded.org.apache.commons.collections4.set.UnmodifiableSet;

import java.util.Map;
import java.util.Set;

/**
 * Decorates another {@link OrderedBidiMap} to ensure it can't be altered.
 * <p>
 * Attempts to modify it will result in an UnsupportedOperationException.
 *
 * @since 3.0
 * @version $Id: UnmodifiableOrderedBidiMap.java 1533984 2013-10-20 21:12:51Z tn $
 */
public final class UnmodifiableOrderedBidiMap<K, V>
        extends AbstractOrderedBidiMapDecorator<K, V> implements Unmodifiable {

    /** The inverse unmodifiable map */
    private UnmodifiableOrderedBidiMap<V, K> inverse;

    /**
     * Factory method to create an unmodifiable map.
     * <p>
     * If the map passed in is already unmodifiable, it is returned.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param map  the map to decorate, must not be null
     * @return an unmodifiable OrderedBidiMap
     * @throws IllegalArgumentException if map is null
     * @since 4.0
     */
    public static <K, V> OrderedBidiMap<K, V> unmodifiableOrderedBidiMap(
            final OrderedBidiMap<? extends K, ? extends V> map) {
        if (map instanceof Unmodifiable) {
            @SuppressWarnings("unchecked") // safe to upcast
            final OrderedBidiMap<K, V> tmpMap = (OrderedBidiMap<K, V>) map;
            return tmpMap;
        }
        return new UnmodifiableOrderedBidiMap<K, V>(map);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     *
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if map is null
     */
    @SuppressWarnings("unchecked") // safe to upcast
    private UnmodifiableOrderedBidiMap(final OrderedBidiMap<? extends K, ? extends V> map) {
        super((OrderedBidiMap<K, V>) map);
    }

    //-----------------------------------------------------------------------
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V put(final K key, final V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> mapToCopy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(final Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        final Set<Entry<K, V>> set = super.entrySet();
        return UnmodifiableEntrySet.unmodifiableEntrySet(set);
    }

    @Override
    public Set<K> keySet() {
        final Set<K> set = super.keySet();
        return UnmodifiableSet.unmodifiableSet(set);
    }

    @Override
    public Set<V> values() {
        final Set<V> set = super.values();
        return UnmodifiableSet.unmodifiableSet(set);
    }

    //-----------------------------------------------------------------------
    @Override
    public K removeValue(final Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OrderedBidiMap<V, K> inverseBidiMap() {
        return inverseOrderedBidiMap();
    }

    //-----------------------------------------------------------------------
    @Override
    public OrderedMapIterator<K, V> mapIterator() {
        final OrderedMapIterator<K, V> it = decorated().mapIterator();
        return UnmodifiableOrderedMapIterator.unmodifiableOrderedMapIterator(it);
    }

    /**
     * Gets an unmodifiable view of this map where the keys and values are reversed.
     *
     * @return an inverted unmodifiable bidirectional map
     */
    public OrderedBidiMap<V, K> inverseOrderedBidiMap() {
        if (inverse == null) {
            inverse = new UnmodifiableOrderedBidiMap<V, K>(decorated().inverseBidiMap());
            inverse.inverse = this;
        }
        return inverse;
    }

}
