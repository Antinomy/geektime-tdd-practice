package geektime.tdd.di;

import geektime.tdd.di.testData.*;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Nested
public class InjectionTest {
    private Dependency dependency = mock(Dependency.class);
    private Provider<Dependency> dependencyProvider = mock(Provider.class);

    private ParameterizedType parameterizedType;

    private Context context = mock(Context.class);

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        parameterizedType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();

        when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(dependency));

        when(context.get(eq(ComponentRef.of(parameterizedType)))).thenReturn(Optional.of(dependencyProvider));

    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {
            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                TestComponent instance = new InjectionProvider<>(InjectionTestData.ComponentDefaultConstructor.class).get(context);

                assertNotNull(instance);
            }


            @Test
            public void should_inject_dependency_via_inject_constructor() {
                InjectionTestData.MissingDependencyConstructor instance = new InjectionProvider<>(InjectionTestData.MissingDependencyConstructor.class).get(context);

                assertNotNull(instance);
                assertSame(dependency, instance.getDependency());
            }

            @Test
            public void should_inject_dependency_via_transitive_constructor() {
                String indirect_dependency = "indirect dependency";
                when(context.get(eq(ComponentRef.of(Dependency.class)))).thenReturn(Optional.of(new DependencyWithInjectConstructor(indirect_dependency)));

                InjectionTestData.MissingDependencyConstructor instance = new InjectionProvider<>(InjectionTestData.MissingDependencyConstructor.class).get(context);

                assertNotNull(instance);

                Dependency dependency = instance.getDependency();
                assertNotNull(dependency);
                assertEquals(indirect_dependency, ((DependencyWithInjectConstructor) dependency).getDependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<InjectionTestData.MissingDependencyConstructor> provider = new InjectionProvider<>(InjectionTestData.MissingDependencyConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {

                InjectionTestData.ProviderInjectConstructor instance = new InjectionProvider<>(InjectionTestData.ProviderInjectConstructor.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }


            @Test
            public void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<InjectionTestData.ProviderInjectConstructor> provider = new InjectionProvider<>(InjectionTestData.ProviderInjectConstructor.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(parameterizedType)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


        }

        @Nested
        class IllegalInjection {

            @ParameterizedTest(name = "supporting {0}")
            @MethodSource
            public void should_throw_ex_if_component_illegal(Class<? extends TestComponent> component){
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(component));
            }

            @Test
            public static Stream<Arguments> should_throw_ex_if_component_illegal() {
                return Stream.of(
                        Arguments.of(org.junit.jupiter.api.Named.of("should_throw_ex_if_multi_inject_constructor", InjectionTestData.ComponentWithMultiInjectConstructor.class)),
                        Arguments.of(org.junit.jupiter.api.Named.of("should_throw_ex_if_no_inject_nor_default_constructor", InjectionTestData.ComponentWithNoInjectConstructor.class)),
                        Arguments.of(org.junit.jupiter.api.Named.of("should_throw_ex_if_component_is_abstract", InjectionTestData.AbstractComponent.class)),
                        Arguments.of(org.junit.jupiter.api.Named.of("should_throw_ex_if_component_is_interface", TestComponent.class))
                );
            }

        }

        @Nested
        public class WithQualifier {
            @BeforeEach
            public void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne")))))
                        .thenReturn(Optional.of(dependency));
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectionTestData.InjectConstructor> provider = new InjectionProvider<>(InjectionTestData.InjectConstructor.class);

                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))},
                        provider.getDependencies().toArray());
            }

            @Test
            public void should_include_dependency_with_qualifier_via_constructor() {
                InjectionProvider<InjectionTestData.InjectConstructor> provider = new InjectionProvider<>(InjectionTestData.InjectConstructor.class);

                InjectionTestData.InjectConstructor component = provider.get(context);
                assertSame(
                        component.dependency,
                        dependency);
            }

            @Test
            public void should_throw_ex_if_multi_qualifiers_given() {

                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(QualifierTestData.MultiQualifierInjectConstructor.class).get(context));
            }
        }
    }

    @Nested
    public class FieldInjection {

        @Nested
        class Injection {

            @Test
            public void should_inject_dependency_via_field() {
                InjectionTestData.ComponentWithFieldInjection component = new InjectionProvider<>(InjectionTestData.ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_super_inject_field() {
                InjectionTestData.SubClassWithFieldInjection component = new InjectionProvider<>(InjectionTestData.SubClassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }


            @Test
            public void should_inject_provider_via_inject_field() {
                InjectionTestData.ProviderInjectField instance = new InjectionProvider<>(InjectionTestData.ProviderInjectField.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            @Test
            public void should_include_field_dependency_in_dependencies() {
                InjectionProvider<InjectionTestData.ComponentWithFieldInjection> provider = new InjectionProvider<>(InjectionTestData.ComponentWithFieldInjection.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }

            @Test
            public void should_include_provider_type_from_inject_field() {
                InjectionProvider<InjectionTestData.ProviderInjectField> provider = new InjectionProvider<>(InjectionTestData.ProviderInjectField.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(parameterizedType)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


        }


        @Nested
        class IllegalInjection {
            @Test
            public void should_throw_ex_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectionTestData.FinalInjectField.class));
            }
        }


        @Nested
        public class WithQualifier {
            @BeforeEach
            public void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne")))))
                        .thenReturn(Optional.of(dependency));
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectionTestData.InjectField> provider = new InjectionProvider<>(InjectionTestData.InjectField.class);

                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))},
                        provider.getDependencies().toArray());
            }

            @Test
            public void should_include_dependency_with_qualifier_via_field() {
                InjectionProvider<InjectionTestData.InjectField> provider = new InjectionProvider<>(InjectionTestData.InjectField.class);

                InjectionTestData.InjectField component = provider.get(context);
                assertSame(
                        component.dependency,
                        dependency);
            }

            @Test
            public void should_throw_ex_if_multi_qualifiers_given() {

                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(QualifierTestData.MultiQualifierInjectField.class).get(context));
            }
        }
    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);

                assertTrue(component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_with_dependency_method() {


                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);

                assertSame(dependency, component.dependency);
            }


            @Test
            public void should_include_dependency_with_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(Dependency.class)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_provider_via_inject_method() {

                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);
                assertSame(dependencyProvider, instance.dependency);
            }

            @Test
            public void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new ComponentRef[]{ComponentRef.of(parameterizedType)}, provider.getDependencies().toArray(ComponentRef[]::new));
            }


            static class SuperClassWithInjectMethod {
                boolean superCalled = false;
                int superCounter = 0;

                @Inject
                void install() {
                    superCalled = true;
                    superCounter++;
                }
            }

            static class SubClassWithInjectMethod extends SuperClassWithInjectMethod {
                boolean subCalled = false;
                int subCounter = 0;

                @Inject
                void installAnother() {
                    subCalled = true;
                    subCounter = superCounter + 1;

                }
            }

            @Test
            public void should_inject_dependencies_via_inject_method_from_superClass() {

                SubClassWithInjectMethod component = new InjectionProvider<>(SubClassWithInjectMethod.class).get(context);

                assertTrue(component.subCalled);
                assertTrue(component.superCalled);

                assertEquals(1, component.superCounter);
                assertEquals(2, component.subCounter);
            }

            static class SubClassOverrideSuperClassWithInjectMethod extends SuperClassWithInjectMethod {

                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_inject_method() {

                SubClassOverrideSuperClassWithInjectMethod component = new InjectionProvider<>(SubClassOverrideSuperClassWithInjectMethod.class).get(context);

                assertEquals(1, component.superCounter);
            }

            static class SubClassOverrideSuperClassWithoutInjectMethod extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_no_call_once_if_subclass_without_override_inject_method() {
                SubClassOverrideSuperClassWithoutInjectMethod component = new InjectionProvider<>(SubClassOverrideSuperClassWithoutInjectMethod.class).get(context);

                assertEquals(0, component.superCounter);
            }
        }

        @Nested
        class IllegalInjection {
            static class InjectMethodWithTypeParam {

                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_ex_if_inject_method_has_type_param() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParam.class));
            }

        }

        @Nested
        public class WithQualifier {
            @BeforeEach
            public void before() {
                Mockito.reset(context);
                when(context.get(eq(ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne")))))
                        .thenReturn(Optional.of(dependency));
            }


            static class InjectMethod {
                Dependency dependency;

                @Inject
                public void install(@Named("ChosenOne") Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_include_dependency_with_qualifier() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);

                assertArrayEquals(
                        new ComponentRef[]{ComponentRef.of(Dependency.class, new NamedLiteral("ChosenOne"))},
                        provider.getDependencies().toArray());
            }

            @Test
            public void should_include_dependency_with_qualifier_via_method() {
                InjectionProvider<InjectMethod> provider = new InjectionProvider<>(InjectMethod.class);

                InjectMethod component = provider.get(context);
                assertSame(
                        component.dependency,
                        dependency);
            }

            static class MultiQualifierInjectMethod {
                Dependency dependency;

                @Inject
                void install(@Named("ChosenOne") @Skywalker Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_throw_ex_if_multi_qualifiers_given() {

                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiQualifierInjectMethod.class).get(context));
            }
        }

    }

}

