package org.atpfivt.comparison.intellij;

import java.io.File;
import java.util.List;
import java.util.stream.Stream;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * A light Java project with the real JDK of the test (the test framework ships no mock JDK) and the JUnit jars that the
 * build passes in {@code test.project.libraries}, so that test classes resolve as in a real project.
 */
public abstract class JavaProjectTestCase extends LightJavaCodeInsightFixtureTestCase {

    private static final String JDK = System.getProperty("java.home");
    private static final List<File> JUNIT = Stream.of(System.getProperty("test.project.libraries").split(File.pathSeparator))
        .map(File::new)
        .toList();

    private static final LightProjectDescriptor PROJECT = new DefaultLightProjectDescriptor(
        () -> JavaSdk.getInstance().createJdk("jdk", JDK)) {

        @Override
        public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model,
                @NotNull ContentEntry contentEntry) {
            super.configureModule(module, model, contentEntry);
            for (File jar : JUNIT) {
                PsiTestUtil.addLibrary(model, jar.getName(), jar.getParent(), jar.getName());
            }
        }
    };

    private final Disposable roots = Disposer.newDisposable();

    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(roots, JDK);
        VfsRootAccess.allowRootAccess(roots, JUNIT.stream().map(File::getParent).toArray(String[]::new));
        super.setUp();
        myFixture.addClass("package org.atpfivt.comparison; public @interface ComparisonTree {}");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            super.tearDown();
        }
        finally {
            Disposer.dispose(roots);
        }
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return PROJECT;
    }
}
