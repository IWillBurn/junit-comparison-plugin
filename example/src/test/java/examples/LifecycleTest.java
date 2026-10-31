/*
 * Lifecycle of a @ParameterizedClass, with output: the plugin keeps every line under its test.
 *
 * Jupiter runs the whole class with Good, then the whole class with Buggy: instances #1..#3 get Good, #4..#6 Buggy.
 * Every test gets a new instance and its own @BeforeEach/@AfterEach. @BeforeAll runs once, the argument source
 * numbers() once per implementation.
 */
package examples;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.atpfivt.comparison.ComparisonTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
@ComparisonTree
class LifecycleTest {

    private static int instances;

    private final int instance = ++instances;

    @Parameter
    Calc calc;

    @BeforeAll
    static void beforeAll() {
        System.out.println("@BeforeAll");
    }

    @BeforeEach
    void setUp() {
        System.out.println("@BeforeEach  instance #" + instance + ", " + calc.getClass().getSimpleName());
    }

    @AfterEach
    void tearDown() {
        System.out.println("@AfterEach   instance #" + instance);
    }

    static Stream<Arguments> numbers() {
        System.out.println("source       numbers()");
        return Stream.of(Arguments.of(2, 3, 6), Arguments.of(4, 5, 20));
    }

    @ParameterizedTest
    @MethodSource("numbers")
    void mul(int a, int b, int expected) {
        System.out.println("test         mul(" + a + ", " + b + ")");
        assertEquals(expected, calc.mul(a, b));
    }

    @Test
    void mulTwoByTwo() {
        System.out.println("test         mulTwoByTwo");
        assertEquals(4, calc.mul(2, 2));
    }
}
