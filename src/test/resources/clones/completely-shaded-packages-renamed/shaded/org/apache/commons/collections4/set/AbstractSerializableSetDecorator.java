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
package shaded.org.apache.commons.collections4.set;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Set;

/**
 * Serializable subclass of AbstractSetDecorator.
 *
 * @since 3.1
 * @version $Id: AbstractSerializableSetDecorator.java 1543167 2013-11-18 21:21:32Z ggregory $
 */
public abstract class AbstractSerializableSetDecorator<E>
        extends AbstractSetDecorator<E> {

    /** Serialization version */
    private static final long serialVersionUID = 1229469966212206107L;

    /**
     * Constructor.
     *
     * @param set  the list to decorate, must not be null
     * @throws IllegalArgumentException if set is null
     */
    protected AbstractSerializableSetDecorator(final Set<E> set) {
        super(set);
    }

    //-----------------------------------------------------------------------
    /**
     * Write the set out using a custom routine.
     *
     * @param out  the output stream
     * @throws IOException
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(decorated());
    }

    /**
     * Read the set in using a custom routine.
     *
     * @param in  the input stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        setCollection((Collection<E>) in.readObject());
    }

}
