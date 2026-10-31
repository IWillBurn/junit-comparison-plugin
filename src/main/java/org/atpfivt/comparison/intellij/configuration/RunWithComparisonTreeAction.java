package org.atpfivt.comparison.intellij.configuration;

import java.util.Objects;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runs the class or method under the caret (or of the gutter icon) with a comparison-tree configuration. IntelliJ's
 * own Run action does not offer one as soon as an ordinary JUnit configuration for the same class exists: it reuses
 * that configuration. This action always uses (or creates) the comparison-tree one.
 */
public final class RunWithComparisonTreeAction extends AnAction {

    static final String ID = "org.atpfivt.comparison.tree.Run";

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ConfigurationFromContext configuration = configuration(e);
        e.getPresentation().setEnabledAndVisible(configuration != null);
        if (configuration != null) {
            e.getPresentation().setText("Run '" + configuration.getConfiguration().getName() + "'", false);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        ConfigurationFromContext configuration = configuration(e);
        if (configuration == null) {
            return;
        }
        RunnerAndConfigurationSettings settings = configuration.getConfigurationSettings();
        RunManager runManager = RunManager.getInstance(Objects.requireNonNull(e.getProject()));
        if (runManager.findSettings(settings.getConfiguration()) == null) {
            runManager.setTemporaryConfiguration(settings); // like a run from the gutter: temporary, selected
        }
        else {
            runManager.setSelectedConfiguration(settings);
        }
        ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
    }

    static @Nullable ConfigurationFromContext configuration(@NotNull AnActionEvent e) {
        ConfigurationContext context = ConfigurationContext.getFromContext(e.getDataContext(), e.getPlace());
        return context.getLocation() == null ? null : configuration(context);
    }

    static @Nullable ConfigurationFromContext configuration(@NotNull ConfigurationContext context) {
        return RunConfigurationProducer.getInstance(ComparisonConfigurationProducer.class)
                .findOrCreateConfigurationFromContext(context);
    }
}
