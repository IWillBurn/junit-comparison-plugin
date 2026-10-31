package org.atpfivt.comparison.intellij.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Approach (a): a run configuration type of its own. Its configurations are ordinary JUnit configurations whose test
 * console parses the runner output with {@link TransposingEventsConverter}.
 */
public final class ComparisonConfigurationType extends ConfigurationTypeBase {

    static final String ID = "JUnitComparisonTree";

    public ComparisonConfigurationType() {
        super(ID, "JUnit (comparison tree)",
            "JUnit run that shows @ParameterizedClass invocations as leaves under each method",
            AllIcons.RunConfigurations.Junit);
        addFactory(new Factory(this));
    }

    static ComparisonConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(ComparisonConfigurationType.class);
    }

    ConfigurationFactory factory() {
        return getConfigurationFactories()[0];
    }

    private static final class Factory extends ConfigurationFactory {

        Factory(ComparisonConfigurationType type) {
            super(type);
        }

        @Override
        public @NotNull String getId() {
            return ID;
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new ComparisonJUnitConfiguration("", project, this);
        }
    }
}
