package geektime.tdd.di.testData;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

public class InjectionTestData {

    abstract public static class AbstractComponent implements TestComponent {

        @Inject
        public AbstractComponent() {
        }
    }

    public static class ComponentWithNoInjectConstructor implements TestComponent {


        public ComponentWithNoInjectConstructor(String a) {
        }


        public ComponentWithNoInjectConstructor(String a, Double b) {
        }
    }

    public static class ProviderInjectConstructor {
        public Provider<Dependency> dependency;

        @Inject
        public ProviderInjectConstructor(Provider<Dependency> dependency) {
            this.dependency = dependency;
        }
    }

    public static class InjectConstructor {
        public Dependency dependency;

        @Inject
        public InjectConstructor(@Named("ChosenOne") Dependency dependency) {
            this.dependency = dependency;
        }
    }

    public static class ComponentWithFieldInjection {
        @Inject
        public Dependency dependency;
    }

    public static class SubClassWithFieldInjection extends ComponentWithFieldInjection {
    }

    public static class ProviderInjectField {
        @Inject
        public Provider<Dependency> dependency;
    }

    public static class FinalInjectField {
        @Inject
        public final Dependency dependency = null;
    }

    public static class InjectField {
        @Inject
        @Named("ChosenOne")
        public Dependency dependency;
    }


    public static class ComponentWithMultiInjectConstructor implements TestComponent {

        @Inject
        public ComponentWithMultiInjectConstructor(String a) {
        }

        @Inject
        public ComponentWithMultiInjectConstructor(String a, Double b) {
        }
    }

    public static class ComponentDefaultConstructor implements TestComponent {
        public ComponentDefaultConstructor() {

        }
    }

    public static class MissingDependencyConstructor implements TestComponent {
        public Dependency getDependency() {
            return dependency;
        }

        private Dependency dependency;

        @Inject
        public MissingDependencyConstructor(Dependency dependency) {
            this.dependency = dependency;
        }
    }
}



