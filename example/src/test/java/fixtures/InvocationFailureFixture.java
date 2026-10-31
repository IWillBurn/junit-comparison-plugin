package fixtures;

import examples.Calc;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

/** The invocation itself fails for Buggy: no tests run for it, so its leaf hangs directly under the class. */
@ParameterizedClass(name = "{argumentSetName}")
@MethodSource("examples.Calc#implementations")
public class InvocationFailureFixture {

    // constructor injection: a field-injected argument does not reach @BeforeParameterizedClassInvocation
    InvocationFailureFixture(Calc calc) {
    }

    @BeforeParameterizedClassInvocation
    static void check(Calc calc) {
        if (calc instanceof Calc.Buggy) {
            throw new IllegalStateException("Buggy is not supported");
        }
    }

    @Test
    void test() {
    }
}
