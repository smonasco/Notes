package org.shannon.function;

/**
 * Some supplier that throws.
 *
 * @param <E>       What kind of Throwable the supplier might throw
 * @param <T>       What type is supplied by the supplier
 */
@FunctionalInterface
public interface ExceptionalSupplier<E extends Throwable, T> {
    T get() throws E;
}
