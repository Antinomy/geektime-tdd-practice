package geektime.tdd.di.testData;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

public record TestLiteral() implements Test {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}
