package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ComponentProvider<T> {


    private Injectable<Constructor<T>> injectConstructor;
    private List<Injectable<Method>> injectMethods;
    private List<Injectable<Field>> injectFields;


    static record Injectable<Element extends AccessibleObject>(Element element, ComponentRef<?>[] required) {
         static <Element extends Executable>Injectable<Element> of(Element element) {
            ComponentRef<?>[] required = stream(element.getParameters()).map(Injectable::toComponentRef).toArray(ComponentRef<?>[]::new);
            return new Injectable<>(element, required);
        }

         static Injectable<Field> of(Field field) {
            return new Injectable<>(field, new ComponentRef<?>[]{toComponentRef(field)});
        }

        Object[] toDependencies(Context context) {
            return stream(required).map(context::get).map(Optional::get).toArray();
        }

        private static ComponentRef toComponentRef(Field p) {
            Annotation qualifier = getQualifier(p);
            return ComponentRef.of(p.getGenericType(),qualifier);
        }

        private static ComponentRef toComponentRef(Parameter p) {
            return ComponentRef.of(p.getParameterizedType(), getQualifier(p));
        }

    };


    public InjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) {
            throw new IllegalComponentException();
        }

        this.injectConstructor = getInjectConstructor(component);

        this.injectMethods = getInjectMethods(component);

        this.injectFields = getInjectFields(component);


        if (injectFields.stream().map(Injectable::element).anyMatch(f -> Modifier.isFinal(f.getModifiers()))) {
            throw new IllegalComponentException();
        }

        if (injectMethods.stream().map(Injectable::element).anyMatch(m -> m.getTypeParameters().length != 0)) {
            throw new IllegalComponentException();
        }
    }



    @Override
    public T get(Context context) {
        try {
            T result = injectConstructor.element.newInstance(injectConstructor.toDependencies(context));

            for (Injectable<Field>  field : injectFields) {
                field.element.set(result, field.toDependencies(context)[0]);
            }

            for (Injectable<Method> method : injectMethods) {
                method.element.invoke(result, method.toDependencies(context));
            }

            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ComponentRef> getDependencies() {
        Stream constructStream = stream(injectConstructor.required);
        Stream fieldStream = injectFields.stream().flatMap(f -> stream(f.required));
        Stream methodStream = injectMethods.stream().flatMap(m -> stream(m.required));

        return concat(concat(constructStream, fieldStream), methodStream).toList();
    }


    private static Annotation getQualifier(AnnotatedElement element) {
        List<Annotation> qualifiers = stream(element.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).toList();
        if(qualifiers.size() >1) throw new IllegalComponentException();

        Annotation qualifier = qualifiers.stream().findFirst().orElse(null);
        return qualifier;
    }


    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields).filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean hasOverride(Method m, Method o) {
        return o.getName().equals(m.getName())
                && Arrays.equals(o.getParameterTypes(), m.getParameterTypes());
    }


    private static <T> boolean hasOverrideByNoInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(
                m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> hasOverride(m, o));
    }

    private static boolean hasOverrideByInjectMethod(List<Method> result, Method m) {
        return result.stream().noneMatch(o -> hasOverride(m, o));
    }


    private static <Type> Constructor<Type> getDefaultConstructor(Class<Type> impl) {
        try {
            return impl.getDeclaredConstructor();
        } catch (Exception e) {
            throw new IllegalComponentException();
        }
    }

    private static<T> List<Injectable<Field>> getInjectFields(Class<T> component) {
        List<Field> injectFields = traverse(component, (fields, current) -> injectable(current.getDeclaredFields()).toList());
        return injectFields.stream().map(Injectable::of).toList();
    }

    private static<T> List<Injectable<Method>> getInjectMethods(Class<T> component) {
        List<Method> result = traverse(
                component, (methods, current) ->
                        injectable(current.getDeclaredMethods())
                                .filter(m -> hasOverrideByInjectMethod(methods, m))
                                .filter(m -> hasOverrideByNoInjectMethod(component, m))
                                .toList()
        );
        Collections.reverse(result);
        return result.stream().map(Injectable::of).toList();
    }

    private static <T> Injectable<Constructor<T>> getInjectConstructor(Class<T> component) {
        Constructor<T> result;

        List<Constructor<?>> constructors = injectable(component.getConstructors()).toList();

        if (constructors.size() > 1)
            throw new IllegalComponentException();


        result = (Constructor<T>) constructors.stream()
                .findFirst()
                .orElseGet(() -> getDefaultConstructor(component));

        return Injectable.of(result);
    }

    private static <T> List<T> traverse(Class<?> component, BiFunction<List<T>, Class<?>, List<T>> finder) {
        List<T> results = new ArrayList<>();
        Class<?> current = component;

        while (current != Object.class) {
            results.addAll(finder.apply(results, current));
            current = current.getSuperclass();
        }

        return results;
    }


}
