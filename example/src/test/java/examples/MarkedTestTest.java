/*
 * @ComparisonTree on a single test: in the IntelliJ plugin only mulFourByFive() is shown by implementation, the other
 * test keeps Jupiter's tree.
 *
 * MarkedTestTest
 * ├─ Good
 * │  └─ mulTwoByTwo() ✔
 * ├─ mulFourByFive()
 * │  ├─ Good ✔
 * │  └─ Buggy ✘ expected: <20> but was: <21>
 * └─ Buggy
 *    └─ mulTwoByTwo() ✔
 */
package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.atpfivt.comparison.ComparisonTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
class MarkedTestTest {

    @Parameter
    Calc calc;

    @Test
    void mulTwoByTwo() {
        assertEquals(4, calc.mul(2, 2));
    }

    @Test
    @ComparisonTree
    void mulFourByFive() {
        assertEquals(20, calc.mul(4, 5));
    }
}
