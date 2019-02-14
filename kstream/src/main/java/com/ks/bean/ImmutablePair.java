package com.ks.bean;

import java.util.Objects;

/**
 * element may be null
 *
 * @param <L> left type
 * @param <R> right type
 */
public class ImmutablePair<L, R> {

    private final L left;
    private final R right;

    public static <L, R> ImmutablePair<L, R> of(final L left, final R right) {
        return new ImmutablePair<>(left, right);
    }

    private ImmutablePair(final L left, final R right) {
        this.left = left;
        this.right = right;
    }


    public L getLeft() {
        return left;
    }

    public R getRight() {
        return right;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ImmutablePair<?, ?>) {
            final ImmutablePair<?, ?> other = (ImmutablePair<?, ?>) obj;
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
