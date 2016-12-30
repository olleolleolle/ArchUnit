package com.tngtech.archunit.core;

import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

import static com.tngtech.archunit.core.JavaConstructor.CONSTRUCTOR_NAME;

public abstract class AccessTarget implements HasName.AndFullName, HasOwner<JavaClass> {
    private final String name;
    private final JavaClass owner;
    private final String fullName;

    AccessTarget(JavaClass owner, String name, String fullName) {
        this.name = name;
        this.owner = owner;
        this.fullName = fullName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JavaClass getOwner() {
        return owner;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final AccessTarget other = (AccessTarget) obj;
        return Objects.equals(this.fullName, other.fullName);
    }

    @Override
    public String toString() {
        return "target{" + fullName + '}';
    }

    public static class FieldAccessTarget extends AccessTarget {
        private final JavaClass type;
        private final Supplier<Optional<JavaField>> field;

        FieldAccessTarget(JavaClass owner, String name, JavaClass type, Supplier<Optional<JavaField>> field) {
            super(owner, name, owner.getName() + "." + name);
            this.type = type;
            this.field = Suppliers.memoize(field);
        }

        public JavaClass getType() {
            return type;
        }

        /**
         * @return A field that matches this target, or {@link Optional#absent()} if no matching field was imported.
         */
        public Optional<JavaField> resolve() {
            return field.get();
        }
    }

    public static abstract class CodeUnitCallTarget extends AccessTarget implements HasParameters, CanBeAnnotated {
        private final ImmutableList<JavaClass> parameters;

        CodeUnitCallTarget(JavaClass owner, String name, JavaClassList parameters) {
            super(owner, name, Formatters.formatMethod(owner.getName(), name, parameters));
            this.parameters = ImmutableList.copyOf(parameters);
        }

        @Override
        public JavaClassList getParameters() {
            return new JavaClassList(parameters);
        }

        /**
         * Tries to resolve the targeted method or constructor.
         *
         * @see ConstructorCallTarget#tryResolve()
         * @see MethodCallTarget#resolve()
         */
        public abstract Set<? extends JavaCodeUnit> resolve();

        @Override
        public boolean isAnnotatedWith(Class<? extends Annotation> annotation) {
            for (JavaCodeUnit codeUnit : resolve()) {
                if (codeUnit.isAnnotatedWith(annotation)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class ConstructorCallTarget extends CodeUnitCallTarget {
        private final Supplier<Optional<JavaConstructor>> constructor;

        ConstructorCallTarget(JavaClass owner, JavaClassList parameters, Supplier<Optional<JavaConstructor>> constructor) {
            super(owner, CONSTRUCTOR_NAME, parameters);
            this.constructor = Suppliers.memoize(constructor);
        }

        /**
         * @return A constructor that matches this target, or {@link Optional#absent()} if no matching constructor
         * was imported.
         */
        public Optional<JavaConstructor> tryResolve() {
            return constructor.get();
        }

        @Override
        public Set<? extends JavaCodeUnit> resolve() {
            return tryResolve().asSet();
        }
    }

    public static class MethodCallTarget extends CodeUnitCallTarget {
        private final JavaClass returnType;
        private final Supplier<Set<JavaMethod>> methods;

        MethodCallTarget(JavaClass owner, String name, JavaClassList parameters,
                         JavaClass returnType, Supplier<Set<JavaMethod>> methods) {
            super(owner, name, parameters);
            this.returnType = returnType;
            this.methods = Suppliers.memoize(methods);
        }

        /**
         * Attempts to resolve imported methods that match this target. Note that while usually there is one unique
         * target (if imported), it is possible that the call is ambiguous. For example consider
         * <pre><code>
         * interface A {
         *     void foo();
         * }
         *
         * interface B {
         *     void foo();
         * }
         *
         * interface D extends A, B {}
         *
         * class X {
         *     D d;
         *     // ...
         *     void bar() {
         *         d.foo();
         *     }
         * }
         * </code></pre>
         * While, for any concrete implementation, the compiler will naturally resolve one concrete target to link to,
         * and thus at runtime the called target ist clear, from an analytical point of view the relevant target
         * can't be uniquely identified here. To sum up, the result can be
         * <ul>
         * <li>empty - if no imported method matches the target</li>
         * <li>a single method - if the method was imported and can uniquely be identified</li>
         * <li>several methods - in scenarios where there is no unique method that matches the target</li>
         * </ul>
         * Note that the target would be uniquely determinable, if D would declare <code>void foo()</code> itself.
         *
         * @return Set of matching methods, usually a single target
         */
        public Set<JavaMethod> resolve() {
            return methods.get();
        }

        public JavaClass getReturnType() {
            return returnType;
        }
    }
}