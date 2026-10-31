package fixtures;

import examples.Calc;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** A disabled method and a disabled @Nested class: every implementation reports them as skipped. */
@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
public class DisabledFixture {

    @Parameter
    Calc calc;

    @Test
    void enabled() {
    }

    @Test
    @Disabled("not ready")
    void disabled() {
    }

    @Nested
    @Disabled("not ready either")
    class Later {

        @Test
        void test() {
        }
    }
}
