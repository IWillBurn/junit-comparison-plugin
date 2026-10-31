package org.atpfivt.comparison.intellij.annotation;

import com.intellij.psi.PsiClass;
import org.atpfivt.comparison.intellij.JavaProjectTestCase;

/** Marks looked up in the code by the names JUnit puts into UniqueIds. */
public class AnnotatedMarksTest extends JavaProjectTestCase {

    private PsiClass outer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.addClass("package a; @org.atpfivt.comparison.ComparisonTree public @interface Composed {}");
        myFixture.addClass("package a; @org.atpfivt.comparison.ComparisonTree public class Direct {}");
        myFixture.addClass("package a; @Composed public class ByComposed {}");
        myFixture.addClass("package a; public class Sub extends Direct {}");
        myFixture.addClass("package a; public class Plain { void test() {} }");
        outer = myFixture.addClass("""
                package a;

                import org.atpfivt.comparison.ComparisonTree;

                public class Outer {
                    @ComparisonTree
                    class Inner {
                    }

                    @ComparisonTree
                    <T> void check(int i, String s, int[] a, T t, Outer.Inner n, String... rest) {
                    }

                    @ComparisonTree
                    void mul(int a) {
                    }

                    void mul(long a) {
                    }
                }
                """);
        myFixture.addClass("package a; public class OuterSub extends Outer {}");
    }

    public void testClasses() {
        AnnotatedMarks marks = new AnnotatedMarks(getProject());
        assertTrue(marks.isMarked("a.Direct"));
        assertTrue(marks.isMarked("a.ByComposed"));
        assertTrue(marks.isMarked("a.Sub"));
        assertTrue(marks.isMarked("a.Outer$Inner"));
        assertFalse(marks.isMarked("a.Outer"));
        assertFalse(marks.isMarked("a.Missing"));
    }

    public void testMethodsByTheirUniqueIdValue() {
        String check = "check(int, java.lang.String, [I, java.lang.Object, a.Outer$Inner, [Ljava.lang.String;)";
        assertEquals(check, AnnotatedMarks.uniqueIdValue(outer.findMethodsByName("check", false)[0]));
        AnnotatedMarks marks = new AnnotatedMarks(getProject());
        assertTrue(marks.isMarked("a.Outer", check));
        assertTrue(marks.isMarked("a.Outer", "mul(int)"));
        assertFalse(marks.isMarked("a.Outer", "mul(long)"));
        assertFalse(marks.isMarked("a.Outer", "missing()"));
        assertTrue("inherited, in the id of the subclass", marks.isMarked("a.OuterSub", "mul(int)"));
        assertTrue("package-private from another package", marks.isMarked("b.Other", "a.Outer#mul(int)"));
    }

    public void testWhatTheGutterOffers() {
        assertTrue(AnnotatedMarks.concerns(outer));
        assertTrue(AnnotatedMarks.concerns(outer.findMethodsByName("mul", false)[0]));
        assertFalse(AnnotatedMarks.concerns(outer.findMethodsByName("mul", false)[1]));
        assertTrue(AnnotatedMarks.concerns(outer.findInnerClassByName("Inner", false)));
        PsiClass plain = myFixture.findClass("a.Plain");
        assertFalse(AnnotatedMarks.concerns(plain));
        assertFalse(AnnotatedMarks.concerns(plain.getMethods()[0]));
    }
}
