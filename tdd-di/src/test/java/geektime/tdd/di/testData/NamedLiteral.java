package geektime.tdd.di.testData;

import jakarta.inject.Named;

import java.lang.annotation.Annotation;
import java.util.Objects;

public record NamedLiteral(String value) implements Named {

    @Override
    public Class<? extends Annotation> annotationType() {
        return Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Named named)
            return Objects.equals(value, named.value());

        return false;
    }

    @Override
    public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}
