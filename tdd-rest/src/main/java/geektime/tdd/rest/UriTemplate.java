package geektime.tdd.rest;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface UriTemplate {

    interface MatchResult extends Comparable<MatchResult> {
        String getMatched();

        String getRemaining();

        Map<String, String> getPathParameters();
    }

    Optional<MatchResult> match(String path);
}

class UriTemplateString implements UriTemplate{

    public static final Pattern VARIABLE = Pattern.compile("\\{\\w[\\w\\.-]*\\}");
    private final Pattern pattern;

    public UriTemplateString(String template) {
        pattern = Pattern.compile("("+ variable(template) +")"+"(/.*)?");
    }

    private static String variable(String template) {
        return VARIABLE.matcher(template).replaceAll("([^/]+?)");
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);

        if(!matcher.matches())
            return Optional.empty();

        int count = matcher.groupCount();

        MatchResult result = new MatchResult() {
            @Override
            public String getMatched() {
                return matcher.group(1);
            }

            @Override
            public String getRemaining() {
                return matcher.group(count);
            }

            @Override
            public Map<String, String> getPathParameters() {
                return null;
            }

            @Override
            public int compareTo(MatchResult o) {
                return 0;
            }
        };

        return Optional.of(result);
    }
}
