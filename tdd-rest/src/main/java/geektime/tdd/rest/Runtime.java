package geektime.tdd.rest;

import geektime.tdd.di.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.ext.Providers;

public interface Runtime {
    Providers getProviders();
    ResourceContext createResourceContext(HttpServletRequest req, HttpServletResponse resp);

    Context getApplicationContext();

    ResourceRouter getResourceRouter();
}
