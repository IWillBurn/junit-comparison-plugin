package org.atpfivt.comparison.intellij.console;

import java.io.OutputStream;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import org.atpfivt.comparison.intellij.transpose.Marks;
import org.atpfivt.comparison.intellij.transpose.ServiceMessage;
import org.atpfivt.comparison.intellij.transpose.TreeTransposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A copy of the test process for the test console: the same lifecycle and output, but with the service messages on
 * stdout transposed. Stopping or detaching it stops or detaches the real process.
 *
 * <p>The output arrives in arbitrary chunks, so stdout is cut into lines here. A partial line is held back only while it
 * may still become a service message; any other output is passed on at once.
 */
public final class TransposingProcessHandler extends ProcessHandler {

    private final ProcessHandler process;
    private final TreeTransposer transposer;
    private final StringBuilder pending = new StringBuilder();
    private @Nullable Key<?> pendingType;

    public TransposingProcessHandler(@NotNull ProcessHandler process, @NotNull Marks marks) {
        this.process = process;
        this.transposer = new TreeTransposer(marks);
        process.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                startNotify();
            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (ProcessOutputType.isStdout(outputType)) {
                    stdout(event.getText(), outputType);
                }
                else {
                    output(event.getText(), outputType);
                }
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                finish();
                notifyProcessTerminated(event.getExitCode());
            }
        });
        if (process.isStartNotified()) {
            startNotify();
        }
    }

    private synchronized void stdout(String text, Key<?> type) {
        pending.append(text);
        pendingType = type;
        int newline;
        while ((newline = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, newline + 1);
            pending.delete(0, newline + 1);
            line(line, type);
        }
        if (!pending.isEmpty() && !mayBecomeServiceMessage(pending.toString())) {
            output(pending.toString(), type);
            pending.setLength(0);
        }
    }

    private void line(String line, Key<?> type) {
        if (!ServiceMessage.isServiceMessage(line)) {
            output(line, type);
            return;
        }
        String newline = line.endsWith("\r\n") ? "\r\n" : "\n";
        for (String message : transposer.process(line.trim())) {
            notifyTextAvailable(message + newline, type);
        }
    }

    /** Output that is not a service message, stderr included. */
    private synchronized void output(String text, Key<?> type) {
        for (String message : transposer.beforeOutput()) {
            notifyTextAvailable(message + "\n", ProcessOutputTypes.STDOUT);
        }
        notifyTextAvailable(text, type);
    }

    private synchronized void finish() {
        if (!pending.isEmpty()) {
            line(pending.toString(), pendingType != null ? pendingType : ProcessOutputTypes.STDOUT);
            pending.setLength(0);
        }
        for (String message : transposer.finish()) {
            notifyTextAvailable(message + "\n", ProcessOutputTypes.STDOUT);
        }
    }

    private static boolean mayBecomeServiceMessage(String partial) {
        String text = partial.stripLeading();
        return text.startsWith("##teamcity[") || "##teamcity[".startsWith(text);
    }

    @Override
    protected void destroyProcessImpl() {
        process.destroyProcess();
    }

    @Override
    protected void detachProcessImpl() {
        process.detachProcess();
    }

    @Override
    public boolean detachIsDefault() {
        return process.detachIsDefault();
    }

    @Override
    public @Nullable OutputStream getProcessInput() {
        return process.getProcessInput();
    }
}
