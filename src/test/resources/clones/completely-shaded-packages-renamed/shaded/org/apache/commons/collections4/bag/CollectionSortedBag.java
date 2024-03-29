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
package shaded.org.apache.commons.collections4.bag;

import shaded.org.apache.commons.collections4.SortedBag;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * Decorates another {@link SortedBag} to comply with the Collection contract.
 *
 * @since 4.0
 * @version $Id: CollectionSortedBag.java 1543168 2013-11-18 21:22:43Z ggregory $
 */
public final class CollectionSortedBag<E> extends AbstractSortedBagDecorator<E> {

    /** Serialization version */
    private static final long serialVersionUID = -2560033712679053143L;

    /**
     * Factory method to create a sorted bag that complies to the Collection contract.
     *
     * @param <E> the type of the elements in the bag
     * @param bag  the sorted bag to decorate, must not be null
     * @return a SortedBag that complies to the Collection contract
     * @throws IllegalArgumentException if bag is null
     */
    public static <E> SortedBag<E> collectionSortedBag(final SortedBag<E> bag) {
        return new CollectionSortedBag<E>(bag);
    }

    //-----------------------------------------------------------------------
    /**
     * Constructor that wraps (not copies).
     *
     * @param bag  the sorted bag to decorate, must not be null
     * @throws IllegalArgumentException if bag is null
     */
    public CollectionSortedBag(final SortedBag<E> bag) {
        super(bag);
    }

    //-----------------------------------------------------------------------
    /**
     * Write the collection out using a custom routine.
     *
     * @param out  the output stream
     * @throws IOException
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(decorated());
    }

    /**
     * Read the collection in using a custom routine.
     *
     * @param in  the input stream
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ClassCastException if deserialised object has wrong type
     */
    @SuppressWarnings("unchecked") // will throw CCE, see Javadoc
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setCollection((Collection<E>) in.readObject());
    }

    //-----------------------------------------------------------------------
    // Collection interface
    //-----------------------------------------------------------------------

    @Override
    public boolean containsAll(final Collection<?> coll) {
        final Iterator<?> e = coll.iterator();
        while (e.hasNext()) {
            if(!contains(e.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean add(final E object) {
        return add(object, 1);
    }

    @Override
    public boolean addAll(final Collection<? extends E> coll) {
        boolean changed = false;
        final Iterator<? extends E> i = coll.iterator();
        while (i.hasNext()) {
            final boolean added = add(i.next(), 1);
            changed = changed || added;
        }
        return changed;
    }

    @Override
    public boolean remove(final Object object) {
        return remove(object, 1);
    }

    @Override
    public boolean removeAll(final Collection<?> coll) {
        if (coll != null) {
            boolean result = false;
            final Iterator<?> i = coll.iterator();
            while (i.hasNext()) {
                final Object obj = i.next();
                final boolean changed = remove(obj, getCount(obj));
                result = result || changed;
            }
            return result;
        } else {
            // let the decorated bag handle the case of null argument
            return decorated().removeAll(null);
        }
    }

    @Override
    public boolean retainAll(final Collection<?> coll) {
        if (coll != null) {
            boolean modified = false;
            final Iterator<E> e = iterator();
            while (e.hasNext()) {
                if (!coll.contains(e.next())) {
                    e.remove();
                    modified = true;
                }
            }
            return modified;
        } else {
            // let the decorated bag handle the case of null argument
            return decorated().retainAll(null);
        }
    }

    //-----------------------------------------------------------------------
    // Bag interface
    //-----------------------------------------------------------------------

    @Override
    public boolean add(final E object, final int count) {
        decorated().add(object, count);
        return true;
    }

}
