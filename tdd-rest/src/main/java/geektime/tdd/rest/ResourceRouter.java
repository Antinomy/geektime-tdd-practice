package geektime.tdd.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import java.util.Collections;
import java.util.Comparator;
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

        Optional<Result> matched = rootResources.stream()
                .map(resource -> new Result(resource.getUriTemplate().match(path), resource))
                .filter(result -> result.matched.isPresent())
                .sorted(Comparator.comparing(x -> x.matched.get()))
                .findFirst();

        Optional<ResourceMethod> method = matched.flatMap(result ->
                result.resource.matches(result.matched.get().getRemaining(), request.getMethod(),
                Collections.list(request.getHeaders(HttpHeaders.ACCEPT)).toArray(String[]::new), uri));

        GenericEntity<?> entity = method.map(m -> m.call(resourceContext,uri)).get();
        return (OutboundResponse) Response.ok(entity).build();
    }

    record Result(Optional<UriTemplate.MatchResult> matched, RootResource resource) {
    }
}
