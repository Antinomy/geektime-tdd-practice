package geektime.tdd.di.testData;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

public class ContainerTestData {

    public static class InjectConstructor implements Dependency {
        public Dependency dependency;

        @Inject
        public InjectConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

    }

    public static class CyclicDependencyProviderConstructor implements Dependency {
        @Inject
        public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
        }

        ;
    }

    public static class MissingDependencyProviderConstructor implements TestComponent {

        @Inject
        public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {

        }
    }

    public static class MissingDependencyProviderField implements TestComponent {

        @Inject
        Provider<Dependency> dependency;
    }

    public static class MissingDependencyProviderMethod implements TestComponent {

        @Inject
        public void install(Provider<Dependency> dependency) {

        }
    }

    @Singleton
    public static class MissingDependencyScope implements TestComponent {

        @Inject
        Dependency dependency;
    }

    @Singleton
    public static class MissingDependencyProviderScope implements TestComponent {

        @Inject
        Provider<Dependency> dependency;
    }


    public static class AnotherDependencyDependedComponent implements AnotherDependency {
        private TestComponent component;

        @Inject
        public AnotherDependencyDependedComponent(TestComponent component) {
            this.component = component;
        }
    }

    public static class DependencyDependedAnotherDependency implements Dependency {
        private AnotherDependency anotherDependency;

        @Inject
        public DependencyDependedAnotherDependency(AnotherDependency anotherDependency) {
            this.anotherDependency = anotherDependency;
        }
    }

    public static class DependencyDependedComponent implements Dependency {
        private TestComponent component;

        @Inject
        public DependencyDependedComponent(TestComponent component) {
            this.component = component;
        }
    }


}
