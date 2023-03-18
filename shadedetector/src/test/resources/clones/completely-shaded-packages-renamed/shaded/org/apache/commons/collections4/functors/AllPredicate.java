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
package shaded.org.apache.commons.collections4.functors;

import shaded.org.apache.commons.collections4.Predicate;

import java.util.Collection;

import static shaded.org.apache.commons.collections4.functors.FunctorUtils.coerce;
import static shaded.org.apache.commons.collections4.functors.FunctorUtils.validate;
import static shaded.org.apache.commons.collections4.functors.TruePredicate.truePredicate;

/**
 * Predicate implementation that returns true if all the
 * predicates return true.
 * If the array of predicates is empty, then this predicate returns true.
 * <p>
 * NOTE: In versions prior to 3.2 an array size of zero or one
 * threw an exception.
 *
 * @since 3.0
 * @version $Id: AllPredicate.java 1543167 2013-11-18 21:21:32Z ggregory $
 */
public final class AllPredicate<T> extends AbstractQuantifierPredicate<T> {

    /** Serial version UID */
    private static final long serialVersionUID = -3094696765038308799L;

    /**
     * Factory to create the predicate.
     * <p>
     * If the array is size zero, the predicate always returns true.
     * If the array is size one, then that predicate is returned.
     *
     * @param <T> the type that the predicate queries
     * @param predicates  the predicates to check, cloned, not null
     * @return the <code>all</code> predicate
     * @throws IllegalArgumentException if the predicates array is null
     * @throws IllegalArgumentException if any predicate in the array is null
     */
    public static <T> Predicate<T> allPredicate(final Predicate<? super T>... predicates) {
        validate(predicates);
        if (predicates.length == 0) {
            return truePredicate();
        }
        if (predicates.length == 1) {
            return coerce(predicates[0]);
        }

        return new AllPredicate<T>(FunctorUtils.copy(predicates));
    }

    /**
     * Factory to create the predicate.
     * <p>
     * If the collection is size zero, the predicate always returns true.
     * If the collection is size one, then that predicate is returned.
     *
     * @param <T> the type that the predicate queries
     * @param predicates  the predicates to check, cloned, not null
     * @return the <code>all</code> predicate
     * @throws IllegalArgumentException if the predicates array is null
     * @throws IllegalArgumentException if any predicate in the array is null
     */
    public static <T> Predicate<T> allPredicate(final Collection<? extends Predicate<T>> predicates) {
        final Predicate<T>[] preds = validate(predicates);
        if (preds.length == 0) {
            return truePredicate();
        }
        if (preds.length == 1) {
            return preds[0];
        }
        return new AllPredicate<T>(preds);
    }

    /**
     * Constructor that performs no validation.
     * Use <code>allPredicate</code> if you want that.
     *
     * @param predicates  the predicates to check, not cloned, not null
     */
    public AllPredicate(final Predicate<? super T>... predicates) {
        super(predicates);
    }

    /**
     * Evaluates the predicate returning true if all predicates return true.
     *
     * @param object  the input object
     * @return true if all decorated predicates return true
     */
    public boolean evaluate(final T object) {
        for (final Predicate<? super T> iPredicate : iPredicates) {
            if (!iPredicate.evaluate(object)) {
                return false;
            }
        }
        return true;
    }

}
