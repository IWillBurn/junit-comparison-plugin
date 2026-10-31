package fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;

import examples.Calc;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** @Nested inside a @ParameterizedClass: the nested class stays a level of the transposed tree. */
@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
public class NestedFixture {

    @Parameter
    Calc calc;

    @Test
    void mulTwoByTwo() {
        assertEquals(4, calc.mul(2, 2));
    }

    @Nested
    class Inner {

        @Test
        void mulFourByFive() {
            assertEquals(20, calc.mul(4, 5));
        }
    }
}
