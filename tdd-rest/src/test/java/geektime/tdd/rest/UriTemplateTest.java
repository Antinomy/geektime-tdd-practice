package geektime.tdd.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UriTemplateTest {

    @ParameterizedTest
    @CsvSource({
            "/users/{id:[0-9]+},/users/id",
            "/users,/orders"
    })
    public  void should_not_match_path(String pattern, String path) {
        UriTemplateString template = new UriTemplateString(pattern);
        assertTrue(template.match(path).isEmpty());
    }


    @Test
    public void should_return_match_result_if_path_matched(){
        UriTemplateString template = new UriTemplateString("/users");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users",result.getMatched()) ;
        assertEquals("/1",result.getRemaining()) ;
        assertTrue(result.getPathParameters().isEmpty());
    }

    @Test
    public void should_return_match_result_if_path_with_variable_matched(){
        UriTemplateString template = new UriTemplateString("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users/1",result.getMatched()) ;
        assertNull(result.getRemaining()) ;
        assertFalse(result.getPathParameters().isEmpty());
        assertEquals("1",result.getPathParameters().get("id") );
    }



    @Test
    public void should_return_extract_result_if_matched_given_pattern(){
        UriTemplateString template = new UriTemplateString("/users/{id:[0-9]+}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals("/users/1",result.getMatched()) ;
        assertNull(result.getRemaining()) ;
        assertFalse(result.getPathParameters().isEmpty());
        assertEquals("1",result.getPathParameters().get("id") );
    }

    @Test
    public void should_throw_illegal_argument_exception_if_variable_redefined(){
        assertThrows(IllegalArgumentException.class ,()-> {
             new UriTemplateString("/users/{id:[0-9]+}/{id}");
        });
    }


    @ParameterizedTest
    @CsvSource({
            "/users/1234,/users/1234,/users/{id}",
            "/users/1234567890/order,/{resources}/1234567890/{action},/users/{id}/order",
            "/users/1,/users/{id:[0-9]+},/users/{id}"
    })
    public  void should_smaller_when_template_smaller(String path, String smallTemplate, String largerTemplate) {
        UriTemplate smaller = new UriTemplateString(smallTemplate);
        UriTemplate larger = new UriTemplateString(largerTemplate);

        UriTemplate.MatchResult lhs = smaller.match(path).get();
        UriTemplate.MatchResult rhs = larger.match(path).get();

        assertTrue( lhs.compareTo(rhs) < 0);
        assertTrue( rhs.compareTo(lhs) > 0);
    }

    @Test
    public void should_compare_equal_matched_result(){
        UriTemplateString template = new UriTemplateString("/users/{id}");

        UriTemplate.MatchResult result = template.match("/users/1").get();

        assertEquals(0,result.compareTo(result) );
    }



}
