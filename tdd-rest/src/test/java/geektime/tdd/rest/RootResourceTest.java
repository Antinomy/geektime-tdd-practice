package geektime.tdd.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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

        @GET
        @Path("/topics/{id}")
        @Produces(MediaType.TEXT_PLAIN)
        public String topicId(){
            return "topicId";
        }

        @GET
        @Path("/topics/1234")
        @Produces(MediaType.TEXT_PLAIN)
        public String topic1234(){
            return "topic1234";
        }

        @POST
        @Path("/hello")
        @Produces(MediaType.TEXT_PLAIN)
        public String postHello(){
            return "hello";
        }
    }

    @Test
    public void should_get_uri_template_from_path_annotation(){
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate template = resource.getUriTemplate();

        assertTrue(template.match("/messages/hello").isPresent()) ;
    }


    @ParameterizedTest
    @CsvSource({
        "GET,/messages/ah,Messages.ah",
        "GET,/messages/hello,Messages.hello",
        "POST,/messages/hello,Messages.postHello",
        "GET,/messages/topics/1234,Messages.topic1234",
        "GET,/messages/topics/12345,Messages.topicId",
    })
    public  void should_match(String httpMethod, String path, String resourceMethod) {
        ResourceRouter.RootResource resource = new RootResourceClass(Messages.class);
        UriTemplate.MatchResult result = resource.getUriTemplate().match(path).get();
        ResourceRouter.ResourceMethod method = resource.matches(result, httpMethod, new String[]{MediaType.TEXT_PLAIN}, mock(UriInfoBuilder.class)).get();
        assertEquals(resourceMethod,method.toString());
    }
}
