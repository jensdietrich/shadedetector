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
package shaded.org.apache.commons.collections4.queue;

import shaded.org.apache.commons.collections4.collection.AbstractCollectionDecorator;

import java.util.Queue;

/**
 * Decorates another {@link Queue} to provide additional behaviour.
 * <p>
 * Methods are forwarded directly to the decorated queue.
 *
 * @param <E> the type of the elements in the queue
 * @since 4.0
 * @version $Id: AbstractQueueDecorator.java 1477765 2013-04-30 18:37:37Z tn $
 */
public abstract class AbstractQueueDecorator<E> extends AbstractCollectionDecorator<E>
        implements Queue<E> {

    /** Serialization version */
    private static final long serialVersionUID = -2629815475789577029L;

    /**
     * Constructor only used in deserialization, do not use otherwise.
     */
    protected AbstractQueueDecorator() {
        super();
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param queue  the queue to decorate, must not be null
     * @throws IllegalArgumentException if list is null
     */
    protected AbstractQueueDecorator(final Queue<E> queue) {
        super(queue);
    }

    /**
     * Gets the queue being decorated.
     *
     * @return the decorated queue
     */
    @Override
    protected Queue<E> decorated() {
        return (Queue<E>) super.decorated();
    }

    //-----------------------------------------------------------------------

    public boolean offer(final E obj) {
        return decorated().offer(obj);
    }

    public E poll() {
        return decorated().poll();
    }

    public E peek() {
        return decorated().peek();
    }

    public E element() {
        return decorated().element();
    }

    public E remove() {
        return decorated().remove();
    }

}
