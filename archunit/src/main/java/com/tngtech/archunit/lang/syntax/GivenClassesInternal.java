package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import com.tngtech.archunit.lang.syntax.elements.ClassesShouldConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClasses;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesThat;

class GivenClassesInternal extends AbstractGivenObjects<JavaClass, GivenClassesInternal>
        implements GivenClasses, GivenClassesConjunction {

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer) {
        this(priority, classesTransformer, Functions.<ArchCondition<JavaClass>>identity());
    }

    GivenClassesInternal(Priority priority, ClassesTransformer<JavaClass> classesTransformer,
                         Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition) {
        this(priority, classesTransformer, prepareCondition, new PredicateAggregator<JavaClass>(), Optional.<String>absent());
    }

    private GivenClassesInternal(
            Priority priority,
            ClassesTransformer<JavaClass> classesTransformer,
            Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition,
            PredicateAggregator<JavaClass> relevantObjectsPredicates,
            Optional<String> overriddenDescription) {

        super(new GivenClassesFactory(),
                priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);

    }

    @Override
    public ClassesShould should() {
        return new ClassesShouldInternal(finishedClassesTransformer(), priority, prepareCondition);
    }

    @Override
    public GivenClassesThat and() {
        return that();
    }

    @Override
    public GivenClassesThat that() {
        return new GivenClassesThatInternal(this);
    }

    @Override
    public ClassesShouldConjunction should(ArchCondition<JavaClass> condition) {
        return new ClassesShouldInternal(finishedClassesTransformer(), priority, condition, prepareCondition);
    }

    private static class GivenClassesFactory implements Factory<JavaClass, GivenClassesInternal> {
        @Override
        public GivenClassesInternal create(Priority priority,
                                           ClassesTransformer<JavaClass> classesTransformer,
                                           Function<ArchCondition<JavaClass>, ArchCondition<JavaClass>> prepareCondition,
                                           PredicateAggregator<JavaClass> relevantObjectsPredicates,
                                           Optional<String> overriddenDescription) {
            return new GivenClassesInternal(
                    priority, classesTransformer, prepareCondition, relevantObjectsPredicates, overriddenDescription);
        }
    }
}