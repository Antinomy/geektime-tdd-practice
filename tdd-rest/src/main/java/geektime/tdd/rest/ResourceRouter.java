package geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ResourceRouter {
    OutboundResponse dispatch(HttpServletRequest request, ResourceContext resourceContext);

    interface Resource {
        Optional<ResourceMethod> matches(String path, String method, String[] mediaTypes, UriInfoBuilder builder);
    }

    interface RootResource extends Resource{
        UriTemplate getUriTemplate();
    }

    interface ResourceMethod{
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
            return resource.matches(matched.get().getRemaining(), request.getMethod(),
                    Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri);
        }
    }
}
