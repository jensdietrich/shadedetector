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

import shaded.org.apache.commons.collections4.BidiMap;
import shaded.org.apache.commons.collections4.MapIterator;
import shaded.org.apache.commons.collections4.map.AbstractMapDecorator;

import java.util.Set;

/**
 * Provides a base decorator that enables additional functionality to be added
 * to a BidiMap via decoration.
 * <p>
 * Methods are forwarded directly to the decorated map.
 * <p>
 * This implementation does not perform any special processing with the map views.
 * Instead it simply returns the set/collection from the wrapped map. This may be
 * undesirable, for example if you are trying to write a validating implementation
 * it would provide a loophole around the validation.
 * But, you might want that loophole, so this class is kept simple.
 *
 * @since 3.0
 * @version $Id: AbstractBidiMapDecorator.java 1543168 2013-11-18 21:22:43Z ggregory $
 */
public abstract class AbstractBidiMapDecorator<K, V>
        extends AbstractMapDecorator<K, V> implements BidiMap<K, V> {

    /**
     * Constructor that wraps (not copies).
     *
     * @param map  the map to decorate, must not be null
     * @throws IllegalArgumentException if the collection is null
     */
    protected AbstractBidiMapDecorator(final BidiMap<K, V> map) {
        super(map);
    }

    /**
     * Gets the map being decorated.
     *
     * @return the decorated map
     */
    @Override
    protected BidiMap<K, V> decorated() {
        return (BidiMap<K, V>) super.decorated();
    }

    //-----------------------------------------------------------------------
    @Override
    public MapIterator<K, V> mapIterator() {
        return decorated().mapIterator();
    }

    public K getKey(final Object value) {
        return decorated().getKey(value);
    }

    public K removeValue(final Object value) {
        return decorated().removeValue(value);
    }

    public BidiMap<V, K> inverseBidiMap() {
        return decorated().inverseBidiMap();
    }

    @Override
    public Set<V> values() {
        return decorated().values();
    }

}
