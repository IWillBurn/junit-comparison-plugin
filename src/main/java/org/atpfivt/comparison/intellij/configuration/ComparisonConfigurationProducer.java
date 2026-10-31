package org.atpfivt.comparison.intellij.configuration;

import java.util.List;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.junit.AllInPackageConfigurationProducer;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.PatternConfigurationProducer;
import com.intellij.execution.junit.TestInClassConfigurationProducer;
import com.intellij.execution.junit.UniqueIdConfigurationProducer;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Offers a comparison-tree configuration wherever JUnit offers one (gutter icon, class, package, a node of the test
 * tree): it asks JUnit's own producers and copies the settings they produce.
 */
public final class ComparisonConfigurationProducer extends LazyRunConfigurationProducer<ComparisonJUnitConfiguration> {

    // the order JUnit prefers: a node of the results tree, then a class or method, then patterns and packages
    private static final List<Class<? extends RunConfigurationProducer<?>>> JUNIT_PRODUCERS = List.of(
        UniqueIdConfigurationProducer.class, TestInClassConfigurationProducer.class,
        PatternConfigurationProducer.class, AllInPackageConfigurationProducer.class);

    @Override
    public @NotNull ConfigurationFactory getConfigurationFactory() {
        return ComparisonConfigurationType.getInstance().factory();
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull ComparisonJUnitConfiguration configuration,
            @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        for (Class<? extends RunConfigurationProducer<?>> type : JUNIT_PRODUCERS) {
            ConfigurationFromContext junit = RunConfigurationProducer.getInstance(type).createConfigurationFromContext(context);
            if (junit != null && junit.getConfiguration() instanceof JUnitConfiguration settings) {
                Element state = new Element("configuration");
                settings.writeExternal(state);
                configuration.readExternal(state);
                configuration.setName(settings.getName() + " (comparison tree)");
                sourceElement.set(junit.getSourceElement());
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public boolean isConfigurationFromContext(@NotNull ComparisonJUnitConfiguration configuration,
            @NotNull ConfigurationContext context) {
        for (Class<? extends RunConfigurationProducer<?>> type : JUNIT_PRODUCERS) {
            RunConfigurationProducer junit = RunConfigurationProducer.getInstance(type); // all take a JUnitConfiguration
            if (junit.isConfigurationFromContext(configuration, context)) {
                return true;
            }
        }
        return false;
    }
}
