package org.atpfivt.comparison.intellij.configuration;

import java.util.List;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.lineMarker.RunLineMarkerProvider;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionWithDelegate;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.atpfivt.comparison.intellij.JavaProjectTestCase;

/**
 * JUnit 5 test classes in a Java project: the gutter icon of a marked one offers the comparison tree, which runs that
 * class; unmarked tests do not get it.
 */
public class GutterTest extends JavaProjectTestCase {

    public void testGutterMenuOffersTheComparisonTree() {
        PsiClass test = myFixture.addClass("""
                package examples;

                import org.atpfivt.comparison.ComparisonTree;
                import org.junit.jupiter.api.Test;

                @ComparisonTree
                public class MulTest {
                    @Test
                    void mulFourByFive() {
                    }
                }
                """);
        assertTrue(offersTheComparisonTree(test.getNameIdentifier()));
        assertTrue(offersTheComparisonTree(test.getMethods()[0].getNameIdentifier()));

        ConfigurationFromContext configuration =
            RunWithComparisonTreeAction.configuration(new ConfigurationContext(test.getNameIdentifier()));
        assertNotNull(configuration);
        ComparisonJUnitConfiguration comparison =
            assertInstanceOf(configuration.getConfiguration(), ComparisonJUnitConfiguration.class);
        assertEquals("MulTest (comparison tree)", comparison.getName());
        assertEquals(JUnitConfiguration.TEST_CLASS, comparison.getPersistentData().TEST_OBJECT);
        assertEquals("examples.MulTest", comparison.getPersistentData().getMainClassName());
    }

    public void testOnlyTestsThatConcernTheMarkGetIt() {
        PsiClass plain = myFixture.addClass("""
                package examples;

                import org.junit.jupiter.api.Test;

                public class PlainTest {
                    @Test
                    void test() {
                    }
                }
                """);
        PsiClass partly = myFixture.addClass("""
                package examples;

                import org.atpfivt.comparison.ComparisonTree;
                import org.junit.jupiter.api.Test;

                public class PartlyTest {
                    @Test
                    void unmarked() {
                    }

                    @Test
                    @ComparisonTree
                    void marked() {
                    }
                }
                """);
        assertFalse(offersTheComparisonTree(plain.getNameIdentifier()));
        assertFalse(offersTheComparisonTree(plain.getMethods()[0].getNameIdentifier()));
        assertTrue(offersTheComparisonTree(partly.getNameIdentifier()));
        assertFalse(offersTheComparisonTree(partly.findMethodsByName("unmarked", false)[0].getNameIdentifier()));
        assertTrue(offersTheComparisonTree(partly.findMethodsByName("marked", false)[0].getNameIdentifier()));
    }

    private static boolean offersTheComparisonTree(PsiElement identifier) {
        LineMarkerInfo<?> marker = new RunLineMarkerProvider().getLineMarkerInfo(identifier);
        assertNotNull("the IDE's run icon", marker);
        ActionGroup menu = ((LineMarkerInfo.LineMarkerGutterIconRenderer<?>) marker.createGutterRenderer())
                .getPopupMenuActions();
        AnAction ours = ActionManager.getInstance().getAction(RunWithComparisonTreeAction.ID);
        return List.of(menu.getChildren(null)).stream()
                .anyMatch(a -> a instanceof ActionWithDelegate<?> w && w.getDelegate() == ours);
    }
}
