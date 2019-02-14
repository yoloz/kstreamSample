package com.ks.bean;

import java.util.Objects;

/**
 * @param <L> left type
 * @param <R> right type
 */
public class MutablePair<L, R> {

    /**
     * Left object
     */
    private L left;
    /**
     * Right object
     */
    private R right;

    /**
     * <p>Obtains an immutable pair of from two objects inferring the generic types.</p>
     *
     * <p>This factory allows the pair to be created using inference to
     * obtain the generic types.</p>
     *
     * @param <L>   the left element type
     * @param <R>   the right element type
     * @param left  the left element, may be null
     * @param right the right element, may be null
     * @return a pair formed from the two parameters, not null
     */
    public static <L, R> MutablePair<L, R> of(final L left, final R right) {
        return new MutablePair<>(left, right);
    }

    /**
     * Create a new pair instance.
     *
     * @param left  the left value, may be null
     * @param right the right value, may be null
     */
    private MutablePair(final L left, final R right) {
        this.left = left;
        this.right = right;
    }

    //-----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public L getLeft() {
        return left;
    }

    /**
     * Sets the left element of the pair.
     *
     * @param left the new value of the left element, may be null
     */
    public void setLeft(final L left) {
        this.left = left;
    }

    /**
     * {@inheritDoc}
     */
    public R getRight() {
        return right;
    }

    /**
     * Sets the right element of the pair.
     *
     * @param right the new value of the right element, may be null
     */
    public void setRight(final R right) {
        this.right = right;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof MutablePair<?, ?>) {
            final MutablePair<?, ?> other = (MutablePair<?, ?>) obj;
            return Objects.equals(getLeft(), other.getLeft())
                    && Objects.equals(getRight(), other.getRight());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getLeft() == null ? 0 : getLeft().hashCode()) ^
                (getRight() == null ? 0 : getRight().hashCode());
    }

    @Override
    public String toString() {
        return "(" + getLeft() + "," + getRight() + ")";
    }
}
