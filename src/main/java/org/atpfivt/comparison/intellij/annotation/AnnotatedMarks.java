package org.atpfivt.comparison.intellij.annotation;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.intellij.codeInsight.MetaAnnotationUtil;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.TypeConversionUtil;
import org.atpfivt.comparison.intellij.transpose.Marks;
import org.jetbrains.annotations.NotNull;

/**
 * The marks from the code: classes and methods annotated with {@code @ComparisonTree}, directly or through an
 * annotation of their own; classes also through a supertype. Each class and method is looked up once per run, so the
 * tree of a run stays consistent; while the IDE indexes, nothing is marked.
 */
public final class AnnotatedMarks implements Marks {

    public static final String ANNOTATION = "org.atpfivt.comparison.ComparisonTree";
    private static final Set<String> ANNOTATIONS = Set.of(ANNOTATION);

    private final Project project;
    private final Map<String, Boolean> known = new ConcurrentHashMap<>();

    public AnnotatedMarks(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public boolean isMarked(String className) {
        return known.computeIfAbsent(className, key -> read(() -> {
            PsiClass type = find(className);
            return type != null && isAnnotated(type);
        }));
    }

    @Override
    public boolean isMarked(String className, String method) {
        return known.computeIfAbsent(className + "#" + method, key -> read(() -> {
            // JUnit names the declaring class of a package-private method from another package: a.Base#m()
            int hash = method.indexOf('#');
            boolean declaringClass = hash >= 0 && hash < method.indexOf('(');
            PsiClass type = find(declaringClass ? method.substring(0, hash) : className);
            String signature = declaringClass ? method.substring(hash + 1) : method;
            String name = signature.substring(0, Math.max(0, signature.indexOf('(')));
            return type != null && Arrays.stream(type.findMethodsByName(name, true))
                    .anyMatch(m -> uniqueIdValue(m).equals(signature) && isAnnotated(m));
        }));
    }

    public static boolean isAnnotated(@NotNull PsiClass type) {
        return MetaAnnotationUtil.isMetaAnnotatedInHierarchy(type, ANNOTATIONS);
    }

    public static boolean isAnnotated(@NotNull PsiMethod method) {
        return MetaAnnotationUtil.isMetaAnnotated(method, ANNOTATIONS);
    }

    /**
     * Whether running the class or method can show a comparison tree: it, an enclosing class or (for a class) a member
     * is marked.
     */
    public static boolean concerns(@NotNull PsiMember member) {
        if (member instanceof PsiMethod method && isAnnotated(method)) {
            return true;
        }
        for (PsiClass type = member instanceof PsiClass c ? c : member.getContainingClass(); type != null;
                type = type.getContainingClass()) {
            if (isAnnotated(type)) {
                return true;
            }
        }
        return member instanceof PsiClass type && PsiTreeUtil.findChildrenOfAnyType(type, PsiMethod.class, PsiClass.class)
                .stream().anyMatch(m -> m instanceof PsiMethod method ? isAnnotated(method) : isAnnotated((PsiClass) m));
    }

    /** The method as JUnit names it in UniqueIds: {@code name(types)}, the erased types by {@code Class#getName}. */
    static String uniqueIdValue(PsiMethod method) {
        return Arrays.stream(method.getParameterList().getParameters()).map(PsiParameter::getType)
                .map(AnnotatedMarks::className).collect(Collectors.joining(", ", method.getName() + "(", ")"));
    }

    private static String className(PsiType type) {
        PsiType erased = TypeConversionUtil.erasure(type);
        return erased instanceof PsiArrayType array ? "[" + descriptor(array.getComponentType()) : name(erased);
    }

    /** As in the name of an array class: {@code I}, {@code Ljava.lang.String;}, {@code [I}. */
    private static String descriptor(PsiType component) {
        if (component instanceof PsiArrayType array) {
            return "[" + descriptor(array.getComponentType());
        }
        return component instanceof PsiPrimitiveType ? ClassUtil.getBinaryPresentation(component)
                : "L" + name(component) + ";";
    }

    private static String name(PsiType type) {
        return type instanceof PsiClassType classType && classType.resolve() instanceof PsiClass resolved
                ? ClassUtil.getJVMClassName(resolved) // a.Outer$Inner
                : type.getCanonicalText(); // primitives
    }

    private PsiClass find(String className) {
        return ClassUtil.findPsiClassByJVMName(PsiManager.getInstance(project), className);
    }

    private boolean read(Computable<Boolean> lookup) {
        if (project.isDisposed() || DumbService.isDumb(project)) {
            return false;
        }
        try {
            return ReadAction.compute(lookup::compute);
        }
        catch (IndexNotReadyException e) {
            return false;
        }
    }
}
