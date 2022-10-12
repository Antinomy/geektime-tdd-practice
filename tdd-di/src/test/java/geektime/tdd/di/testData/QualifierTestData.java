package geektime.tdd.di.testData;

import jakarta.inject.Inject;
import jakarta.inject.Named;

public class QualifierTestData {

    public static class MultiQualifierInjectConstructor {
        Dependency dependency;

        @Inject
        public MultiQualifierInjectConstructor(@Named("ChosenOne") @Skywalker Dependency dependency) {
            this.dependency = dependency;
        }
    }

    public static class MultiQualifierInjectField {

        @Inject
        @Named("ChosenOne")
        @Skywalker
        Dependency dependency;

    }

}
