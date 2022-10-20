package geektime.tdd.rest;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class ResourceDispatchTest {

    HttpServletRequest request;

    ResourceContext context;

    RuntimeDelegate delegate;

    Runtime runtime;

    UriInfoBuilder builder;

    @BeforeEach
    public void setup(){
        runtime = mock(Runtime.class);

        delegate = mock(RuntimeDelegate.class);
        RuntimeDelegate.setInstance(delegate);

        when(delegate.createResponseBuilder()).thenReturn(new StubRBuilder());


        request = mock(HttpServletRequest.class);
        context = mock(ResourceContext.class);
        when(request.getServletPath()).thenReturn("/users/1");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeaders(eq(HttpHeaders.ACCEPT))).thenReturn(new Vector<>(List.of(MediaType.WILDCARD)).elements());

        builder = mock(UriInfoBuilder.class);
        when(runtime.createUriInfoBuilder(same(request))).thenReturn(builder);
    }

    @Test
    public void should_use_matched_root_resource(){
        GenericEntity<String> entity = new GenericEntity("matched",String.class);

        ResourceRouter.RootResource matched = rootResource(matched("/users/1", result("/1")), returns(entity));
        ResourceRouter.RootResource unmatched = rootResource(unmatched("/users/1"));

        ResourceRouter router = new DefaultResourceRouter(runtime,List.of(matched,unmatched));

        OutboundResponse response = router.dispatch(request,context);
        assertSame(entity,response.getGenericEntity());
        assertEquals(200,response.getStatus());
    }

    @Test
    public void should_sort_matched_root_resource_descending_order(){
        GenericEntity<String> entity1 = new GenericEntity("1",String.class);
        GenericEntity<String> entity2 = new GenericEntity("2",String.class);

        ResourceRouter.RootResource matched2 = rootResource(matched("/users/1", result("/1",2)), returns(entity2));
        ResourceRouter.RootResource matched1 = rootResource(matched("/users/1", result("/1",1)), returns(entity1));

        ResourceRouter router = new DefaultResourceRouter(runtime,List.of(matched2,matched1));

        OutboundResponse response = router.dispatch(request,context);
        assertSame(entity1,response.getGenericEntity());
        assertEquals(200,response.getStatus());
    }

    @Test
    public void should_return_404_if_no_resource(){
        ResourceRouter.RootResource unmatched = rootResource(unmatched("/users/1"));

        ResourceRouter router = new DefaultResourceRouter(runtime,List.of(unmatched));

        OutboundResponse response = router.dispatch(request,context);
        assertNull(response.getGenericEntity());
        assertEquals(404,response.getStatus());
    }

    @Test
    public void should_return_404_if_no_method_matched(){
        ResourceRouter.RootResource unmatched = rootResource(matched("/users/1", result("/1",2)));

        ResourceRouter router = new DefaultResourceRouter(runtime,List.of(unmatched));

        OutboundResponse response = router.dispatch(request,context);
        assertNull(response.getGenericEntity());
        assertEquals(404,response.getStatus());
    }

    @Test
    public void should_return_204_if_no_resource_return_matched(){
        ResourceRouter.RootResource unmatched = rootResource(matched("/users/1", result("/1",2)),returns(null));

        ResourceRouter router = new DefaultResourceRouter(runtime,List.of(unmatched));

        OutboundResponse response = router.dispatch(request,context);
        assertNull(response.getGenericEntity());
        assertEquals(204,response.getStatus());
    }

    private static ResourceRouter.RootResource rootResource(UriTemplate uriTemplate) {
        ResourceRouter.RootResource unmatched = mock(ResourceRouter.RootResource.class);
        when(unmatched.getUriTemplate()).thenReturn(uriTemplate);
        return unmatched;
    }

    private static UriTemplate unmatched(String path) {
        UriTemplate unmatchedUrlTemplate = mock(UriTemplate.class);
        when(unmatchedUrlTemplate.match(path)).thenReturn(Optional.empty());
        return unmatchedUrlTemplate;
    }

    private ResourceRouter.RootResource rootResource(UriTemplate matchedUrlTemplate, ResourceRouter.ResourceMethod method) {
        ResourceRouter.RootResource matched = mock(ResourceRouter.RootResource.class);
        when(matched.getUriTemplate()).thenReturn(matchedUrlTemplate);
        when(matched.matches(eq("/1"), eq("GET"), eq(new String[]{MediaType.WILDCARD}), eq(builder)))
                .thenReturn(Optional.of(method));
        return matched;
    }

    private ResourceRouter.ResourceMethod returns(GenericEntity<String> entity) {
        ResourceRouter.ResourceMethod method = mock(ResourceRouter.ResourceMethod.class);
        when(method.call(same(context), same(builder))).thenReturn(entity);
        return method;
    }

    private static UriTemplate matched(String path, UriTemplate.MatchResult result) {
        UriTemplate matchedUrlTemplate = mock(UriTemplate.class);
        when(matchedUrlTemplate.match(path)).thenReturn(Optional.of(result));
        return matchedUrlTemplate;
    }

    private static UriTemplate.MatchResult result(String path) {
        UriTemplate.MatchResult result = mock(UriTemplate.MatchResult.class);
        when(result.getRemaining()).thenReturn(path);
        return result;
    }

    private  UriTemplate.MatchResult result(String path, Integer order) {

        return new FakeMathResult(path,order);
    }

    class FakeMathResult implements UriTemplate.MatchResult{

        private String remaining;
        private Integer order;

        public FakeMathResult(String remaining, Integer order) {
            this.remaining = remaining;
            this.order = order;
        }

        @Override
        public String getMatched() {
            return null;
        }

        @Override
        public String getRemaining() {
            return remaining;
        }

        @Override
        public Map<String, String> getPathParameters() {
            return null;
        }

        @Override
        public int compareTo(UriTemplate.MatchResult o) {
            return order.compareTo(((FakeMathResult)o).order);
        }
    }

    private class StubRBuilder extends Response.ResponseBuilder {
        private int status;
        private Object entity;

        @Override
        public Response build() {
            OutboundResponse response = mock(OutboundResponse.class);
            when(response.getStatus()).thenReturn(status);
            when(response.getEntity()).thenReturn(entity);
            when(response.getGenericEntity()).thenReturn((GenericEntity) entity);
            return response;
        }

        @Override
        public Response.ResponseBuilder clone() {
            return null;
        }

        @Override
        public Response.ResponseBuilder status(int i) {
            return null;
        }

        @Override
        public Response.ResponseBuilder status(int i, String s) {
            this.status = i;
            return this;
        }

        @Override
        public Response.ResponseBuilder entity(Object o) {
            this.entity =o;
            return this;
        }

        @Override
        public Response.ResponseBuilder entity(Object o, Annotation[] annotations) {
            return null;
        }

        @Override
        public Response.ResponseBuilder allow(String... strings) {
            return null;
        }

        @Override
        public Response.ResponseBuilder allow(Set<String> set) {
            return null;
        }

        @Override
        public Response.ResponseBuilder cacheControl(CacheControl cacheControl) {
            return null;
        }

        @Override
        public Response.ResponseBuilder encoding(String s) {
            return null;
        }

        @Override
        public Response.ResponseBuilder header(String s, Object o) {
            return null;
        }

        @Override
        public Response.ResponseBuilder replaceAll(MultivaluedMap<String, Object> multivaluedMap) {
            return null;
        }

        @Override
        public Response.ResponseBuilder language(String s) {
            return null;
        }

        @Override
        public Response.ResponseBuilder language(Locale locale) {
            return null;
        }

        @Override
        public Response.ResponseBuilder type(MediaType mediaType) {
            return null;
        }

        @Override
        public Response.ResponseBuilder type(String s) {
            return null;
        }

        @Override
        public Response.ResponseBuilder variant(Variant variant) {
            return null;
        }

        @Override
        public Response.ResponseBuilder contentLocation(URI uri) {
            return null;
        }

        @Override
        public Response.ResponseBuilder cookie(NewCookie... newCookies) {
            return null;
        }

        @Override
        public Response.ResponseBuilder expires(Date date) {
            return null;
        }

        @Override
        public Response.ResponseBuilder lastModified(Date date) {
            return null;
        }

        @Override
        public Response.ResponseBuilder location(URI uri) {
            return null;
        }

        @Override
        public Response.ResponseBuilder tag(EntityTag entityTag) {
            return null;
        }

        @Override
        public Response.ResponseBuilder tag(String s) {
            return null;
        }

        @Override
        public Response.ResponseBuilder variants(Variant... variants) {
            return null;
        }

        @Override
        public Response.ResponseBuilder variants(List<Variant> list) {
            return null;
        }

        @Override
        public Response.ResponseBuilder links(Link... links) {
            return null;
        }

        @Override
        public Response.ResponseBuilder link(URI uri, String s) {
            return null;
        }

        @Override
        public Response.ResponseBuilder link(String s, String s1) {
            return null;
        }
    }



}