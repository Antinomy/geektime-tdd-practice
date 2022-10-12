package geektime.tdd.rest;

import geektime.tdd.di.ComponentRef;
import geektime.tdd.di.Context;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;
import jakarta.ws.rs.ext.RuntimeDelegate;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;



public class ResourceServlet extends HttpServlet {
    private Runtime runtime;
//    private final Context context;
//    private TestApplication application;
//    private Providers providers;

    public ResourceServlet(Runtime runtime) {
        this.runtime = runtime;

//        this.providers = runtime.getProviders();
//        context = runtime.getApplicationContext();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ResourceRouter router = runtime.getResourceRouter();
        Providers providers = runtime.getProviders();

        OutboundResponse response = router.dispatch(req, runtime.createResourceContext(req, resp));
        resp.setStatus(response.getStatus());
        MultivaluedMap<String, Object> headers = response.getHeaders();
        for(String name : headers.keySet()){
            for(Object value:headers.get(name)){
                RuntimeDelegate.HeaderDelegate delegate = RuntimeDelegate.getInstance().createHeaderDelegate(value.getClass());
                resp.addHeader(name, delegate.toString(value));
            }
        }

        GenericEntity entity = response.getGenericEntity();
        MessageBodyWriter writer = providers.getMessageBodyWriter(entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType());
        writer.writeTo(entity.getEntity(),entity.getRawType(), entity.getType(), response.getAnnotations(), response.getMediaType(),
                response.getHeaders(),resp.getOutputStream());
    }
}