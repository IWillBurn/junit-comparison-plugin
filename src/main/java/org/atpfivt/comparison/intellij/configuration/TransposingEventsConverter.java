package org.atpfivt.comparison.intellij.configuration;

import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.openapi.util.Key;
import org.atpfivt.comparison.intellij.annotation.AnnotatedMarks;
import org.atpfivt.comparison.intellij.transpose.Marks;
import org.atpfivt.comparison.intellij.transpose.ServiceMessage;
import org.atpfivt.comparison.intellij.transpose.TreeTransposer;
import org.jetbrains.annotations.NotNull;

/**
 * IntelliJ's own parser of the runner output, fed with transposed service messages. {@code processConsistentText}
 * receives every service message as a separate, complete line.
 */
public final class TransposingEventsConverter extends OutputToGeneralTestEventsConverter {

    private final TreeTransposer transposer;

    public TransposingEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties properties) {
        this(testFrameworkName, properties, new AnnotatedMarks(properties.getProject()));
    }

    public TransposingEventsConverter(@NotNull String testFrameworkName, @NotNull TestConsoleProperties properties,
            @NotNull Marks marks) {
        super(testFrameworkName, properties);
        transposer = new TreeTransposer(marks);
    }

    @Override
    protected void processConsistentText(@NotNull String text, @NotNull Key<?> outputType) {
        if (!ServiceMessage.isServiceMessage(text)) {
            for (String line : transposer.beforeOutput()) {
                super.processConsistentText(line + "\n", ProcessOutputTypes.STDOUT);
            }
            super.processConsistentText(text, outputType);
            return;
        }
        for (String line : transposer.process(text.trim())) {
            super.processConsistentText(line + "\n", outputType);
        }
    }

    /** A single-class run never finishes the class node, so the merged nodes are finished here. */
    @Override
    public void flushBufferOnProcessTermination(int exitCode) {
        super.flushBufferOnProcessTermination(exitCode);
        for (String line : transposer.finish()) {
            super.processConsistentText(line + "\n", ProcessOutputTypes.STDOUT);
        }
    }
}
