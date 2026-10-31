package org.atpfivt.comparison.intellij.transpose;

/**
 * Which test classes and methods carry {@code @ComparisonTree}, asked with the names JUnit puts into UniqueIds. See
 * {@link NodeId#implementations} for what a mark transposes.
 */
public interface Marks {

    /** Every class marked: the invocations of every {@code @ParameterizedClass} are transposed. */
    Marks ALL = new Marks() {

        @Override
        public boolean isMarked(String className) {
            return true;
        }

        @Override
        public boolean isMarked(String className, String method) {
            return false;
        }
    };

    /** @param className binary name, e.g. {@code examples.Outer$Inner} */
    boolean isMarked(String className);

    /** @param method as in the UniqueId: name and parameter types by {@code Class#getName}, e.g. {@code mul(int, int)} */
    boolean isMarked(String className, String method);
}
