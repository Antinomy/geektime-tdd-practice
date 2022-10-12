package geektime.tdd.di;

import geektime.tdd.di.testData.*;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;


import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class ContextTest {

    private ContextConfig config;

    @BeforeEach
    void setUp() {
        config = new ContextConfig();
    }


    @Nested
    public class ComponentConstruction {

        @Nested
        public class TypeBinding {
            @Test
            public void should_bind_type_to_a_spec_instance() {

                TestComponent instance = new TestComponent() {};
                config.instance(TestComponent.class, instance);

                assertSame(instance, config.getContext().get(ComponentRef.of(TestComponent.class)).get());
            }


            @Test
            public void should_retrieve_empty_for_unbind_type() {
                Optional<TestComponent> component = config.getContext().get(ComponentRef.of(TestComponent.class));
                assertTrue(component.isEmpty());
            }


            @Test
            public void should_retrieve_bind_type_as_provider() {
                TestComponent instance = new TestComponent() {};
                config.instance(TestComponent.class, instance);


                Context context = config.getContext();
                Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>(){}).get();
                assertSame(instance,provider.get());

            }

            @Test
            public void should_not_retrieve_bind_type_as_unsupported_container() {
                TestComponent instance = new TestComponent() {};
                config.instance(TestComponent.class, instance);

                Context context = config.getContext();

                assertFalse(context.get(new ComponentRef<List<TestComponent>>(){}).isPresent());
            }

            @Nested
            public class WithQualifier{

                @Test
                public void should_bind_instance_with_qualifier() {
                    TestComponent instance = new TestComponent() {};

                    NamedLiteral chosenOneLit = new NamedLiteral("ChosenOne");
                    config.instance(TestComponent.class, instance, chosenOneLit);
                    Context context = config.getContext();

                    TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, chosenOneLit)).get();
                    assertSame(instance, chosenOne);
                }

                @Test
                public void should_bind_component_with_qualifier() {
                    NamedLiteral chosenOneLit = new NamedLiteral("ChosenOne");
                    Dependency dependency = new Dependency() {};
                    config.instance(Dependency.class, dependency);
                    config.component(ContainerTestData.InjectConstructor.class, ContainerTestData.InjectConstructor.class, chosenOneLit);

                    Context context = config.getContext();

                    ContainerTestData.InjectConstructor chosenOne = context.get(ComponentRef.of(ContainerTestData.InjectConstructor.class, chosenOneLit)).get();
                    assertSame(dependency, chosenOne.dependency);
                }

                @Test
                public void should_bind_instance_with_multi_qualifiers() {
                    TestComponent instance = new TestComponent() {};

                    NamedLiteral chosenOneLit = new NamedLiteral("ChosenOne");

                    config.instance(TestComponent.class, instance, chosenOneLit,new SkywalkerLiteral());
                    Context context = config.getContext();

                    TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, chosenOneLit)).get();
                    TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();

                    assertSame(instance, chosenOne);
                    assertSame(instance, skywalker);
                }

                @Test
                public void should_bind_component_with_multi_qualifiers() {
                    NamedLiteral chosenOneLit = new NamedLiteral("ChosenOne");

                    Dependency dependency = new Dependency() {};
                    config.instance(Dependency.class, dependency);
                    config.component(ContainerTestData.InjectConstructor.class, ContainerTestData.InjectConstructor.class, chosenOneLit,new SkywalkerLiteral());

                    Context context = config.getContext();

                    ContainerTestData.InjectConstructor chosenOne = context.get(ComponentRef.of(ContainerTestData.InjectConstructor.class, chosenOneLit)).get();
                    ContainerTestData.InjectConstructor skywalker = context.get(ComponentRef.of(ContainerTestData.InjectConstructor.class, new SkywalkerLiteral())).get();
                    assertSame(dependency, chosenOne.dependency);
                    assertSame(dependency, skywalker.dependency);
                }

                @Test
                public void should_throw_ex_if_illegal_qualifier_given_to_insance(){
                    TestComponent instance = new TestComponent() {};

                    assertThrows(ContextConfig.ContextConfigException.class, () -> config.instance(TestComponent.class, instance,new TestLiteral()));
                }

                @Test
                public void should_throw_ex_if_illegal_qualifier_given_to_component(){
                    Dependency dependency = new Dependency() {};
                    config.instance(Dependency.class, dependency);

                    assertThrows(ContextConfig.ContextConfigException.class, () -> config.component(ContainerTestData.InjectConstructor.class, ContainerTestData.InjectConstructor.class,new TestLiteral()));
                }
            }

            @Nested
            public class WithScope{

                static class NotSingleton {
                }

                @Test
                public void should_not_be_singleton_scope_by_default() {
                    config.component(NotSingleton.class, NotSingleton.class);
                    Context context = config.getContext();

                    assertNotSame(context.get(ComponentRef.of(NotSingleton.class)).get(),
                            context.get(ComponentRef.of(NotSingleton.class)).get()
                    );
                }

                @Test
                public void should_bind_component_as_singleton_scope() {
                    config.component(NotSingleton.class, NotSingleton.class, new SingletonLiteral());
                    Context context = config.getContext();

                    assertSame(context.get(ComponentRef.of(NotSingleton.class)).get(),
                            context.get(ComponentRef.of(NotSingleton.class)).get()
                    );
                }

                @Test
                public void should_throw_ex_when_bind_component_with_multi_qualifier() {
                    assertThrows( ContextConfig.ContextConfigException.class,()-> {
                        config.component(NotSingleton.class, NotSingleton.class, new SingletonLiteral(),new PooledLiteral());
                    });
                }

                @Singleton @Pooled
                static class MultiScopeAnnotated{ }

                @Test
                public void should_throw_ex_when_bind_component_with_multi_scope() {
                    assertThrows( ContextConfig.ContextConfigException.class,()-> {
                        config.component(MultiScopeAnnotated.class, MultiScopeAnnotated.class);
                    });
                }

                @Test
                public void should_throw_ex_when_scope_undefined() {
                    assertThrows( ContextConfig.ContextConfigException.class,()-> {
                        config.component(NotSingleton.class, NotSingleton.class,new PooledLiteral());
                    });
                }

                @Singleton
                static class SingletonAnnotated {
                }

                @Test
                public void should_bind_component_as_singleton_annotated() {
                    config.component(SingletonAnnotated.class, SingletonAnnotated.class);
                    Context context = config.getContext();

                    assertSame(context.get(ComponentRef.of(SingletonAnnotated.class)).get(),
                            context.get(ComponentRef.of(SingletonAnnotated.class)).get()
                    );
                }

                @Test
                public void should_bind_component_as_customized_scope() {
                    config.scope(Pooled.class,PooledProvider::new);
                    config.component(NotSingleton.class, NotSingleton.class,new PooledLiteral());
                    Context context = config.getContext();
                    List<NotSingleton> instances = IntStream.range(0, 5).mapToObj(i -> context.get(ComponentRef.of(NotSingleton.class)).get()).toList();
                    assertEquals(PooledProvider.MAX, new HashSet<>(instances).size());
                }


                @Nested
                public class WithQualifier {
                    @Test
                    public void should_not_be_singleton_scope_by_default() {
                        config.component(NotSingleton.class, NotSingleton.class, new SkywalkerLiteral());
                        Context context = config.getContext();

                        assertNotSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get(),
                                context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get()
                        );
                    }

                    @Test
                    public void should_bind_component_as_singleton_scope() {
                        config.component(NotSingleton.class, NotSingleton.class, new SingletonLiteral(), new SkywalkerLiteral());
                        Context context = config.getContext();

                        assertSame(context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get(),
                                context.get(ComponentRef.of(NotSingleton.class, new SkywalkerLiteral())).get()
                        );
                    }

                    @Test
                    public void should_bind_component_as_singleton_annotated() {
                        config.component(SingletonAnnotated.class, SingletonAnnotated.class, new SkywalkerLiteral());
                        Context context = config.getContext();

                        assertSame(context.get(ComponentRef.of(SingletonAnnotated.class, new SkywalkerLiteral())).get(),
                                context.get(ComponentRef.of(SingletonAnnotated.class, new SkywalkerLiteral())).get()
                        );
                    }
                }
            }

        }

        @Nested
        public class DependencyCheck {


            public static Stream<Arguments> should_throw_ex_if_dependency_not_found() {
                return Stream.of(
                        Arguments.of(Named.of("Inject Constructor", InjectionTestData.MissingDependencyConstructor.class)),
                        Arguments.of(Named.of("Provider in Inject Constructor", ContainerTestData.MissingDependencyProviderConstructor.class)),
                        Arguments.of(Named.of("Provider in Inject Field", ContainerTestData.MissingDependencyProviderField.class)),
                        Arguments.of(Named.of("Provider in Inject Method", ContainerTestData.MissingDependencyProviderMethod.class)),
                        Arguments.of(Named.of("Scoped", ContainerTestData.MissingDependencyScope.class)),
                        Arguments.of(Named.of("Provider Scoped", ContainerTestData.MissingDependencyProviderScope.class))
                );
            }

            @ParameterizedTest(name = "supporting {0}")
            @MethodSource
            public void should_throw_ex_if_dependency_not_found(Class<? extends TestComponent> componentType) {
                config.component(TestComponent.class, componentType);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });

                assertEquals(Dependency.class, exception.getDependency().type());
                assertEquals(TestComponent.class, exception.getComponent().type());
            }

            @Test
            public void should_throw_ex_if_missing_transitive_dependency() {
                config.component(Dependency.class, DependencyWithInjectConstructor.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });

                assertEquals(String.class, exception.getDependency().type());
                assertEquals(Dependency.class, exception.getComponent().type());
            }

            @Test
            public void should_throw_ex_if_cyclic_dependency() {
                config.component(TestComponent.class, InjectionTestData.MissingDependencyConstructor.class);
                config.component(Dependency.class, ContainerTestData.DependencyDependedComponent.class);

                CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> {
                    config.getContext();
                });

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());
                assertEquals(2, classes.size());
                assertTrue(classes.contains(TestComponent.class));
                assertTrue(classes.contains(Dependency.class));
            }

            @Test
            public void should_throw_ex_if_cyclic_transitive_dependency() {
                config.component(TestComponent.class, InjectionTestData.MissingDependencyConstructor.class);
                config.component(Dependency.class, ContainerTestData.DependencyDependedAnotherDependency.class);
                config.component(AnotherDependency.class, ContainerTestData.AnotherDependencyDependedComponent.class);

                CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> {
                    config.getContext();
                });

                Set<Class<?>> classes = Sets.newSet(exception.getComponents());
                assertEquals(3, classes.size());
                assertTrue(classes.contains(TestComponent.class));
                assertTrue(classes.contains(Dependency.class));
                assertTrue(classes.contains(AnotherDependency.class));
            }

            @Test
            public void should_not_throw_ex_if_cyclic_dependency_via_provider() {
                config.component(TestComponent.class, ContainerTestData.MissingDependencyProviderConstructor.class);
                config.component(Dependency.class, ContainerTestData.CyclicDependencyProviderConstructor.class);

                Context context =  config.getContext();
                assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());

            }

            @Nested
            public class WithQualifier{
                @Test
                public void should_throw_ex_if_dependency_with_qualifier_not_found(){
                    config.instance(Dependency.class, new Dependency() {});
                    config.component(InjectConstructor.class,InjectConstructor.class,new NamedLiteral("Owner"));

                    DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, ()-> config.getContext());

                    assertEquals(new Component(InjectConstructor.class,new NamedLiteral("Owner")),exception.getComponent());
                    assertEquals(new Component(Dependency.class,new SkywalkerLiteral()),exception.getDependency());

                }

                static class InjectConstructor {
                    @Inject
                    public InjectConstructor(@Skywalker Dependency dependency){

                    }
                }

                static class SkywalkerDependency implements Dependency {

                    @Inject
                    public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency){

                    }
                }

                static class NotCyclicDependency implements Dependency {

                    @Inject
                    public NotCyclicDependency(@Skywalker Dependency dependency){

                    }
                }

                @Test
                public void should_not_throw_ex_if_component_with_same_type_tag_with_diff_qualifier(){
                    Dependency instance = new Dependency() {};
                    config.instance(Dependency.class,instance,new NamedLiteral("ChosenOne"));
                    config.component(Dependency.class,SkywalkerDependency.class,new SkywalkerLiteral());
                    config.component(Dependency.class,NotCyclicDependency.class);

                    assertDoesNotThrow(() -> config.getContext());
                }
            }

        }

    }

    @Nested
    public class DependenciesSelection {


    }

    @Nested
    public class LifecycleManagement {

    }

}





