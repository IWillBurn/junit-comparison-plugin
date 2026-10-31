package fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import examples.Calc;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A @Nested @ParameterizedClass inside the implementations: the inner dimension (the factor) becomes a level above the
 * implementation leaves, {@code Inner > mulByFive() > [factor] > implementation}.
 */
@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
public class NestedParameterizedFixture {

    @Parameter
    Calc calc;

    @Nested
    @ParameterizedClass(name = "factor {0}")
    @ValueSource(ints = { 2, 4 })
    class Inner {

        @Parameter
        int factor;

        @Test
        void mulByFive() {
            assertEquals(factor * 5, calc.mul(factor, 5));
        }
    }
}
