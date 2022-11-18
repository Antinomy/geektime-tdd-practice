package geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> matches(UriTemplate.MatchResult result, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource{
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod{
        String getHttpMethod();

        UriTemplate getUriTemplate();

        public GenericEntity call(ResourceContext resourceContext, UriInfoBuilder builder) ;
    }

}

class DefaultResourceRouter implements ResourceRouter{

    private List<RootResource> rootResources;
    private Runtime runtime;

    public DefaultResourceRouter(Runtime runtime, List<RootResource> rootResources) {
        this.rootResources = rootResources;
        this.runtime = runtime;
    }

    @Override
    public OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext) {
        String path = request.getServletPath();

        UriInfoBuilder uri= runtime.createUriInfoBuilder(request);

        Optional<ResourceMethod> method = rootResources.stream().map(resource -> match(path, resource))
                .filter(Result::isMatch).sorted().findFirst()
                .flatMap(result -> result.findResourceMethod(request, uri));

        if(method.isEmpty())
            return (OutboundResponse) Response.status(Response.Status.NOT_FOUND).build();


        return (OutboundResponse) method.map(m -> m.call(resourceContext, uri))
                .map(entity -> Response.ok(entity).build())
                .orElseGet(() -> Response.status(Response.Status.NO_CONTENT).build());

    }


    private  Result match(String path, RootResource resource) {
        return new Result(resource.getUriTemplate().match(path), resource);
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) implements Comparable<Result> {
        private  boolean isMatch() {
            return matched.isPresent();
        }

        @Override
        public int compareTo(Result o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo))
                    .orElse(0);
        }

        private  Optional<ResourceMethod> findResourceMethod(HttpServletRequest request, UriInfoBuilder uri) {
            return resource.matches(matched.get(), request.getMethod(),
                    Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri);
        }
    }
}

class RootResourceClass implements ResourceRouter.RootResource{

    private final PathTemplate uriTemplate;
    private Class<?> resourceClass;

//    private List<ResourceRouter.ResourceMethod> resourceMethods;

    private Map<String ,List<ResourceRouter.ResourceMethod>> resourceMethods;

    public RootResourceClass(Class<?> resourceClass) {
        this.resourceClass = resourceClass;
        this.uriTemplate = new PathTemplate(resourceClass.getAnnotation(Path.class ).value());

        this.resourceMethods = Arrays.stream(resourceClass.getMethods())
                .filter(m -> Arrays.stream(m.getAnnotations()).anyMatch(a -> a.annotationType().isAnnotationPresent(HttpMethod.class)))
                .map(m -> new DefaultResourceMethod(m))
                .collect(Collectors.groupingBy(m ->m.getHttpMethod())) ;
    }

    @Override
    public Optional<ResourceRouter.ResourceMethod> matches(UriTemplate.MatchResult result, String method, String[] mediaTypes, UriInfoBuilder builder) {
//        UriTemplate.MatchResult result = uriTemplate.match(result).get();
        String remaining = result.getRemaining();

       return resourceMethods.get(method).stream()
               .map(m -> match(remaining,m))
               .filter(Result::isMatched).sorted()
               .findFirst()
               .map(Result::resourceMethod);

    }

    @Override
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    private Result match(String path, ResourceRouter.ResourceMethod method){
        return new Result(method.getUriTemplate().match(path),method );
    }

    record Result(Optional<UriTemplate.MatchResult> matched , ResourceRouter.ResourceMethod resourceMethod) implements Comparable<Result>{

        public boolean isMatched(){
            return matched.map(r -> r.getRemaining() == null).orElse(false);
        }

        @Override
        public int compareTo(Result o) {
            return matched.flatMap(x -> o.matched.map(x::compareTo)).orElse(0);
        }
    }

    static class DefaultResourceMethod implements ResourceRouter.ResourceMethod{

        private String httpMethod;
        private Method method;
        private UriTemplate uriTemplate;
        public DefaultResourceMethod(Method method) {
            this.method = method;
            this.uriTemplate = new PathTemplate(method.getAnnotation(Path.class).value());

            this.httpMethod=Arrays.stream(method.getAnnotations())
                    .filter(a -> a.annotationType().isAnnotationPresent(HttpMethod.class))
                    .findFirst().get().annotationType().getAnnotation(HttpMethod.class).value();
        }

        @Override
        public String getHttpMethod() {
            return httpMethod;
        }

        @Override
        public UriTemplate getUriTemplate() {
            return uriTemplate;
        }

        @Override
        public GenericEntity call(ResourceContext resourceContext, UriInfoBuilder builder) {
            return null;
        }

        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName()+"."+method.getName();
        }
    }
}
