package geektime.tdd.di.testData;

import java.lang.annotation.Annotation;



public record PooledLiteral() implements Pooled {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Pooled.class;
    }
}

