package org.atpfivt.comparison;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the invocations of a {@code @ParameterizedClass} as the implementations under comparison: the IntelliJ plugin
 * then shows {@code class > method > [argument set] > implementation} instead of Jupiter's
 * {@code class > implementation > method > [argument set]}. Tests without the mark keep Jupiter's tree.
 *
 * <ul>
 * <li>On a {@code @ParameterizedClass}: its invocations, for every test in it, {@code @Nested} classes included. A
 * {@code @Nested @ParameterizedClass} inside it stays a level unless it is marked too.</li>
 * <li>On a test method or a {@code @Nested} class: only that test or class, compared across the invocations of the
 * nearest enclosing {@code @ParameterizedClass}.</li>
 * </ul>
 *
 * It also works as a meta-annotation and is inherited by subclasses. JUnit itself ignores it.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ComparisonTree {
}
