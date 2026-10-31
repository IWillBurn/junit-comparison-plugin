/*
 * The main example in plain Jupiter: @ParameterizedClass over the implementations, the methods are plain @Test and
 * @ParameterizedTest. Buggy is wrong on 4 * 5.
 *
 * Tree as Jupiter runs it (implementation on top):
 *
 * MulTest
 * ├─ Good
 * │  ├─ mul(int, int, int)
 * │  │  ├─ [1] 2, 3, 6 ✔
 * │  │  └─ [2] 4, 5, 20 ✔
 * │  └─ mulFourByFive() ✔
 * └─ Buggy
 *    ├─ mul(int, int, int)
 *    │  ├─ [1] 2, 3, 6 ✔
 *    │  └─ [2] 4, 5, 20 ✘ expected: <20> but was: <21>
 *    └─ mulFourByFive() ✘ expected: <20> but was: <21>
 *
 * The plugin shows it as "class > method > [argument set] > implementation" during the run, because the class is
 * marked with @ComparisonTree.
 */
package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.atpfivt.comparison.ComparisonTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
@ComparisonTree
class MulTest {

    @Parameter
    Calc calc;

    // quoteTextArguments = false only shortens the display name to "[1] 2, 3, 6" (JUnit 6 default: "2", "3", "6")
    @ParameterizedTest(quoteTextArguments = false)
    @CsvSource({ "2, 3, 6", "4, 5, 20" })
    void mul(int a, int b, int expected) {
        assertEquals(expected, calc.mul(a, b));
    }

    @Test
    void mulFourByFive() {
        assertEquals(20, calc.mul(4, 5));
    }
}
