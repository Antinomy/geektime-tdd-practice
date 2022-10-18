package geektime.tdd.rest;

import java.util.Map;
import java.util.Optional;

interface UriTemplate {

    interface MatchResult extends Comparable<MatchResult> {
        String getMatched();

        String getRemaining();

        Map<String, String> getPathParameters();
    }

    Optional<MatchResult> match(String path);
}
