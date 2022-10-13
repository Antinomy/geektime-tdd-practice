package geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class ResourceServletTest extends ServletTest {

    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;

    private Providers providers;

    private RuntimeDelegate delegate;


    @Override
    protected Servlet getServlet() {
        runtime = mock(Runtime.class);
        router = mock(ResourceRouter.class);
        resourceContext = mock(ResourceContext.class);
        providers = mock(Providers.class);

        when(runtime.getResourceRouter()).thenReturn(router);
        when(runtime.createResourceContext(any(), any())).thenReturn(resourceContext);
        when(runtime.getProviders()).thenReturn(providers);

        return new ResourceServlet(runtime);
    }

    @BeforeEach
    public void beforeEach() {
        delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createHeaderDelegate(eq(NewCookie.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<NewCookie>() {
            @Override
            public NewCookie fromString(String s) {
                return null;
            }

            @Override
            public String toString(NewCookie newCookie) {
                return newCookie.getName() + "=" + newCookie.getValue();
            }
        });
    }


    @Nested
    class RespondForOutboundResponse {
        @Test
        public void should_use_status_from_response() {
            response().status(Response.Status.NOT_MODIFIED).returnFrom(router);

            HttpResponse<String> httpResponse = get("/test");
            assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
        }

        @Test
        public void should_use_http_headers_from_response() {
            response().headers(testHeader()).returnFrom(router);

            HttpResponse<String> httpResponse = get("/test");

            assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                    httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
        }


        @Test
        public void should_write_entity_to_http_response_message_body() {

            response().entity(new GenericEntity<Object>("entity", String.class), new Annotation[0])
                    .returnFrom(router);

            HttpResponse<String> httpResponse = get("/test");
            assertEquals("entity", httpResponse.body());
        }

        @Test
        public void should_use_response_from_web_app_ex() {
            response().status(Response.Status.FORBIDDEN)
                    .entity(new GenericEntity<Object>("error", String.class), new Annotation[0])
                    .headers(testHeader())
                    .throwFrom(router);

            HttpResponse<String> httpResponse = get("/test");

            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
            assertEquals("error", httpResponse.body());
            assertArrayEquals(new String[]{"SESSION_ID=session", "USER_ID=user"},
                    httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new));
        }


        @Test
        public void should_not_call_message_body_writer_if_entity_is_null() throws Exception {
            response().entity(null, new Annotation[0]).returnFrom(router);

            HttpResponse<String> httpResponse = get("/test");
            assertEquals("", httpResponse.body());
            assertEquals(Response.Status.OK.getStatusCode(), httpResponse.statusCode());
        }

        public void should_build_response_by_ex_mapper_from_web_app_ex() throws Exception {

            when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
            when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(ex -> response().status(Response.Status.FORBIDDEN).build());

            HttpResponse<String> httpResponse = get("/test");
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
        }
    }

    @Test


    @TestFactory
    public List<DynamicTest> RespondForException() {
        List<DynamicTest> tests = new ArrayList<>();

        Map<String, Consumer<Consumer<RuntimeException>>> exceptions = Map.of(
                "Other Exception", this::otherExceptionThrowFrom,
                "WebApplicaityon Exception", this::webAppExceptionThrowFrom
        );

        Map<String, Consumer<RuntimeException>> callers = getCallers();

        for (Map.Entry<String, Consumer<RuntimeException>> caller : callers.entrySet())
            for (Map.Entry<String, Consumer<Consumer<RuntimeException>>> ex : exceptions.entrySet())
                tests.add(DynamicTest.dynamicTest(
                        caller.getKey() + " throw " + ex.getKey(),
                        () -> ex.getValue().accept(caller.getValue())
                ));


        return tests;
    }

    private Map<String, Consumer<RuntimeException>> getCallers() {
        Map<String, Consumer<RuntimeException>> callers = new HashMap<>();
        List<Method> methods = Arrays.stream(this.getClass().getDeclaredMethods()).filter(m -> m.isAnnotationPresent(ExceptionThrownFrom.class)).toList();
        for (Method method : methods) {
            String name = method.getName();
            String callerName = name.substring(0, 1).toUpperCase() + name.substring(1).replace("_", ".");
            callers.put(callerName, e -> {
                try {
                    method.invoke(this, e);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            });
        }
        return callers;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface ExceptionThrownFrom {
    }

    @ExceptionThrownFrom
    private void providers_getExceptionMapper(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenReturn(ex -> {
            throw exception;
        });
    }

    @ExceptionThrownFrom
    private void providers_getMessageBodyWriter(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);
        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenThrow(exception);
    }

    @ExceptionThrownFrom
    private void messageBodyWriter_writeTo(RuntimeException exception) {
        response().entity(new GenericEntity<>(2.5, Double.class), new Annotation[0]).returnFrom(router);

        when(providers.getMessageBodyWriter(eq(Double.class), eq(Double.class), eq(new Annotation[0]), eq(MediaType.TEXT_PLAIN_TYPE)))
                .thenReturn(new MessageBodyWriter<Double>() {
                                @Override
                                public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
                                    return false;
                                }

                                @Override
                                public void writeTo(Double aDouble, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
                                    throw exception;
                                }
                            }
                );
    }

    @ExceptionThrownFrom
    private void headerDelegate_toString(RuntimeException exception) {
        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(router);
        when(delegate.createHeaderDelegate(eq(MediaType.class)))
                .thenReturn(new RuntimeDelegate.HeaderDelegate<MediaType>() {
                    @Override
                    public MediaType fromString(String s) {
                        return null;
                    }

                    @Override
                    public String toString(MediaType mediaType) {
                        throw exception;
                    }
                });
    }

    @ExceptionThrownFrom
    private void exceptionMapper_toResponse(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(RuntimeException.class);
        when(providers.getExceptionMapper(eq(RuntimeException.class))).thenThrow(exception);
    }

    @ExceptionThrownFrom
    public void resourceRouter_dispatch(RuntimeException exception) {
        when(router.dispatch(any(), eq(resourceContext))).thenThrow(exception);
    }

    @ExceptionThrownFrom
    private void runtimeDelegate_createHeaderDelegate(RuntimeException exception) {
        response().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_TYPE).returnFrom(router);
        when(delegate.createHeaderDelegate(eq(MediaType.class))).thenThrow(exception);
    }

    private void webAppExceptionThrowFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new WebApplicationException(response().status(Response.Status.FORBIDDEN).build());

        caller.accept(exception);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }


    private void otherExceptionThrowFrom(Consumer<RuntimeException> caller) {
        RuntimeException exception = new IllegalArgumentException();

        caller.accept(exception);

        when(providers.getExceptionMapper(eq(IllegalArgumentException.class))).thenReturn(ex -> response().status(Response.Status.FORBIDDEN).build());
        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), httpResponse.statusCode());
    }

    private OutBoundBuilder response() {
        return new OutBoundBuilder();
    }

    class OutBoundBuilder {
        Response.Status status = Response.Status.OK;
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        GenericEntity<Object> entity = new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutBoundBuilder status(Response.Status status) {
            this.status = status;
            return this;
        }

        public OutBoundBuilder headers(String name, Object... values) {
            this.headers.addAll(name, values);
            return this;
        }

        public OutBoundBuilder headers(MultivaluedMap<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public OutBoundBuilder entity(GenericEntity<Object> entity, Annotation[] annotations) {
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void returnFrom(ResourceRouter router) {
            build(response -> when(router.dispatch(any(), eq(resourceContext))).thenReturn(response));
        }

        void throwFrom(ResourceRouter router) {
            build(response -> {
                WebApplicationException ex = new WebApplicationException(response);
                when(router.dispatch(any(), eq(resourceContext))).thenThrow(ex);
            });
        }

        void build(Consumer<OutboundResponse> consumer) {
            OutboundResponse response = build();

            consumer.accept(response);
        }

        OutboundResponse build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getStatusInfo()).thenReturn(status);
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);

            stubMessageBodyWriter();

            return response;
        }

        private void stubMessageBodyWriter() {
            when(providers.getMessageBodyWriter(eq(String.class), eq(String.class), same(annotations), eq(mediaType))).thenReturn(
                    new MessageBodyWriter<>() {
                        @Override
                        public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
                            return false;
                        }

                        @Override
                        public void writeTo(String s, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException {
                            PrintWriter writer = new PrintWriter(outputStream);
                            writer.write(s);
                            writer.flush();
                        }
                    }
            );
        }
    }

    private static MultivaluedMap<String, Object> testHeader() {
        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie",
                new NewCookie.Builder("SESSION_ID").value("session").build(),
                new NewCookie.Builder("USER_ID").value("user").build());
        return headers;
    }
}
