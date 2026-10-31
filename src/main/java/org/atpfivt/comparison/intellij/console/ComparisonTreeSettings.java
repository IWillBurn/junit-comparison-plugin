package org.atpfivt.comparison.intellij.console;

import com.intellij.ide.util.PropertiesComponent;

/** Whether approach (b) transposes the ordinary JUnit runs; an application-wide switch, on by default. */
final class ComparisonTreeSettings {

    private static final String KEY = "org.atpfivt.comparison.tree.junit";

    private ComparisonTreeSettings() {
    }

    static boolean isEnabled() {
        return PropertiesComponent.getInstance().getBoolean(KEY, true);
    }

    static void setEnabled(boolean enabled) {
        PropertiesComponent.getInstance().setValue(KEY, enabled, true);
    }
}
