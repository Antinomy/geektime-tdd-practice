package geektime.tdd.di.testData;

import jakarta.inject.Singleton;

import java.lang.annotation.Annotation;

public record SingletonLiteral() implements Singleton {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Singleton.class;
    }
}
