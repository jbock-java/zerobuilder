package net.zerobuilder;

/**
 * This interface represents the final step of a telescoping builder.
 *
 * @param <E> type of the object that is being built
 */
public interface HasBuildMethod<E> {

  E build();
}
