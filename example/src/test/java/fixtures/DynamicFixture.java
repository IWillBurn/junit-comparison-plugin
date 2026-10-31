package fixtures;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.stream.Stream;

import examples.Calc;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** The implementations produce different dynamic tests: Good "2 * 2" and "4 * 5", Buggy only "4 * 5". */
@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
public class DynamicFixture {

    @Parameter
    Calc calc;

    @TestFactory
    Stream<DynamicTest> products() {
        Stream<String> names = calc instanceof Calc.Good ? Stream.of("2 * 2", "4 * 5") : Stream.of("4 * 5");
        return names.map(name -> dynamicTest(name, () -> {
        }));
    }
}
