package geektime.tdd.rest;

import java.util.*;
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

class UriTemplateString implements UriTemplate {
    public static final String Remaining = "(/.*)?";
    private final Pattern pattern;
    private PathVariables pathVariables = new PathVariables();
    private int variableGroupStartFrom;


    public UriTemplateString(String template) {
        pattern = Pattern.compile(group(pathVariables.template(template)) + Remaining);
        variableGroupStartFrom = 2;
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);

        if (!matcher.matches())
            return Optional.empty();

        return Optional.of(new PathMatchResult(matcher, pathVariables));
    }

    private static String group(String pattern) {
        return "(" + pattern + ")";
    }

    class PathVariables implements Comparable<PathVariables> {
        private static final String LeftBracket = "\\{";
        private static final String RightBracket = "}";
        private static final String NonBracket = "[^\\{}]+";
        private static final String VariableName = "\\w[\\w\\.-]*";
        public static final String defaultVariablePattern = "([^/]+?)";

        public static Pattern VARIABLE = Pattern.compile(
                LeftBracket
                        + group(VariableName)
                        + group(":" + group(NonBracket)) + "?"
                        + RightBracket
        );

        private final List<String> variables = new ArrayList<>();
        private int specificPatternCount = 0;
        private int variableNameGroup = 1;
        private int variablePatternGroup = 3;

        Map<String, String> parameters =new HashMap<>();

        private Map<String, String> extract(Matcher matcher){
            for (int i = 0; i < pathVariables.variables.size(); i++) {
                parameters.put(pathVariables.variables.get(i), matcher.group(variableGroupStartFrom + i));
            }
           return parameters;
        }


        private String template(String template) {
            return VARIABLE.matcher(template).replaceAll(pathVariables::replace);
        }

        private String replace(java.util.regex.MatchResult result) {

            String var = result.group(variableNameGroup);
            String pattern = result.group(variablePatternGroup);

            if (variables.contains(var))
                throw new IllegalArgumentException("Duplicate Variable" + var);

            variables.add(var);

            if (pattern != null) {
                specificPatternCount++;
                return group(pattern);
            }

            return defaultVariablePattern;

        }

        @Override
        public int compareTo(PathVariables o) {
            if (specificPatternCount > o.specificPatternCount) return -1;
            if (specificPatternCount < o.specificPatternCount) return 1;

            if (parameters.size() > o.parameters.size()) return -1;
            if (parameters.size() < o.parameters.size()) return 1;

            return 0;
        }
    }

    class PathMatchResult implements UriTemplate.MatchResult {
        private Matcher matcher;
        private Map<String, String> parameters;
        private int count;
        private int matchLiteralCount;


        private PathVariables variables;

        public PathMatchResult(Matcher matcher, PathVariables pathVariables) {
            this.matcher = matcher;
            this.count = matcher.groupCount();

            this.variables = pathVariables;
            this.parameters = pathVariables.extract(matcher);

            this.matchLiteralCount = matcher.group(1).length()
                    - parameters.values().stream().map(String::length).reduce(0,(a,b) -> a+b);

        }

        public String getMatched() {
            return matcher.group(1);
        }

        @Override
        public String getRemaining() {
            return matcher.group(count);
        }

        @Override
        public Map<String, String> getPathParameters() {
            return parameters;
        }

        @Override
        public int compareTo(UriTemplate.MatchResult o) {
            PathMatchResult result = (PathMatchResult) o;

            if (matchLiteralCount > result.matchLiteralCount) return -1;
            if (matchLiteralCount < result.matchLiteralCount) return 1;

            return variables.compareTo(result.variables);
        }

    }


}

