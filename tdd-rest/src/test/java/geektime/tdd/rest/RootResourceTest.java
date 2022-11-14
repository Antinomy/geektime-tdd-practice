package geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;


public class RootResourceTest {



    @Path("/messages")
    static class Messages{


        @GET
        @Path("/ah")
        @Produces(MediaType.TEXT_PLAIN)
        public String ah(){
            return "ah";
        }

        @GET
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String hello(){
            return "hello";
        }
    }

    @Test
    public void should_get_uri_template_from_path_annotation(){
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate template = resource.getUriTemplate();

        assertTrue(template.match("/messages/hello").isPresent()) ;
    }

//    @Test
//    public void should_match_resource_method_of_uri_and_http_method_fully_matched(){
//
//
//        String httpMethod = "GET";
//        String path = "/messages/hello";
//        String resourceMethod = "Messages.hello";
//
//        should_match(httpMethod, path, resourceMethod);
//    }

    @ParameterizedTest
    @CsvSource({
        "GET,/messages/ah,Messages.ah",
        "GET,/messages/hello,Messages.hello"
    })
    public  void should_match(String httpMethod, String path, String resourceMethod) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        ResourceRouter.ResourceMethod method = resource.matches(path, httpMethod, new String[]{MediaType.TEXT_PLAIN}, mock(UriInfoBuilder.class)).get();
        assertEquals(resourceMethod,method.toString());
    }
}
