package examples;

import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/** The interface under comparison and its two implementations. */
public interface Calc {

    int mul(int a, int b);

    /** The implementations under comparison, one argument set per implementation. */
    static Stream<Arguments> implementations() {
        return Stream.of(argumentSet("Good", new Good()), argumentSet("Buggy", new Buggy()));
    }

    /** Correct. */
    class Good implements Calc {
        @Override
        public int mul(int a, int b) {
            return a * b;
        }
    }

    /** Wrong on 4 * 5: returns 21. */
    class Buggy implements Calc {
        @Override
        public int mul(int a, int b) {
            return a * b == 20 ? 21 : a * b;
        }
    }
}
