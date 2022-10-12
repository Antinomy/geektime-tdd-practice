package geektime.tdd.rest;

import jakarta.servlet.Servlet;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class ResourceServletTest extends ServletTest {

    private Runtime runtime;
    private ResourceRouter router;
    private ResourceContext resourceContext;

    private Providers providers;

    private OutBoundBuilder builder;


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
    public  void beforeEach() {
        builder = new OutBoundBuilder();
        RuntimeDelegate delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);
        when(delegate.createHeaderDelegate(eq(NewCookie.class))).thenReturn(new RuntimeDelegate.HeaderDelegate<NewCookie>() {
            @Override
            public NewCookie fromString(String s) {
                return null;
            }

            @Override
            public String toString(NewCookie newCookie) {
                return newCookie.getName()+"="+newCookie.getValue();
            }
        });


    }


    @Test
    public void should_use_status_from_response() throws Exception {

        builder.status(Response.Status.NOT_MODIFIED).build(router);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals(Response.Status.NOT_MODIFIED.getStatusCode(), httpResponse.statusCode());
    }

    @Test
    public void should_use_http_headers_from_response() throws Exception {

        MultivaluedMap<String,Object> headers = new MultivaluedHashMap<>();
        headers.addAll("Set-Cookie",
                new NewCookie.Builder("SESSION_ID").value("session").build(),
                new NewCookie.Builder("USER_ID").value("user").build());


        builder.headers(headers).build(router);


        HttpResponse<String> httpResponse = get("/test");

        String[] respHeaders = httpResponse.headers().allValues("Set-Cookie").toArray(String[]::new);
        assertArrayEquals(new String[]{"SESSION_ID=session","USER_ID=user"},respHeaders);
    }

    @Test
    public void should_write_entity_to_http_response_message_body() throws Exception {
        GenericEntity<Object> entity = new GenericEntity<>("entity",String.class);
        Annotation[] annotations = new Annotation[0];

        builder.entity(entity,annotations).build(router);

        HttpResponse<String> httpResponse = get("/test");
        assertEquals("entity",httpResponse.body());
    }

    class OutBoundBuilder{
        Response.Status status = Response.Status.OK;
        MultivaluedMap<String, Object> headers =new MultivaluedHashMap<>();
        GenericEntity<Object> entity= new GenericEntity<>("entity", String.class);
        Annotation[] annotations = new Annotation[0];
        MediaType mediaType = MediaType.TEXT_PLAIN_TYPE;

        public OutBoundBuilder status(Response.Status status){
            this.status = status;
            return this;
        }

        public OutBoundBuilder headers(String name, Object... values){
            this.headers.addAll(name,values);
            return this;
        }

        public OutBoundBuilder headers(MultivaluedMap<String, Object> headers){
            this.headers=headers;
            return this;
        }

        public OutBoundBuilder entity(GenericEntity<Object> entity, Annotation[] annotations){
            this.entity = entity;
            this.annotations = annotations;
            return this;
        }

        void build(ResourceRouter router){
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status.getStatusCode());
            when(response.getHeaders()).thenReturn(headers);
            when(response.getGenericEntity()).thenReturn(entity);
            when(response.getAnnotations()).thenReturn(annotations);
            when(response.getMediaType()).thenReturn(mediaType);
            when(router.dispatch(any(), eq(resourceContext))).thenReturn(response);


            when(providers.getMessageBodyWriter(eq(String.class),eq(String.class),same(annotations),eq(mediaType))).thenReturn(
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
}
