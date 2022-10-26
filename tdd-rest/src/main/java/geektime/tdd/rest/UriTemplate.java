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

class UriTemplateString implements UriTemplate{

    private static final String LeftBracket ="\\{";
    private static final String RightBracket ="}";
    private static final String NonBracket ="[^\\{}]+";
    private static final String VariableName ="\\w[\\w\\.-]*";
    public static final String Remaining = "(/.*)?";
    public static final String defaultVariablePattern = "([^/]+?)";

    public static Pattern VARIABLE = Pattern.compile(
            LeftBracket
                    + group(VariableName)
                    + group(":"+group(NonBracket))+"?"
                    + RightBracket
    );
    private final Pattern pattern;

    private final List<String> variables = new ArrayList<>();
    private int variableGroupStartFrom;
    private int  variableNameGroup = 1;
    private int  variablePatternGroup = 3;

    private static String group(String pattern){
        return "("+pattern+")";
    }


    public UriTemplateString(String template) {
        pattern = Pattern.compile(group(variable(template))+ Remaining);
        variableGroupStartFrom = 2;
    }

    private String variable(String template) {
        return VARIABLE.matcher(template).replaceAll(result -> {

            String var = result.group(variableNameGroup);
            String pattern = result.group(variablePatternGroup);

            if(variables.contains(var))
                throw new IllegalArgumentException("Duplicate Variable" + var);

            variables.add(var);

            return pattern == null ? defaultVariablePattern : group(pattern);
        });
    }

    @Override
    public Optional<MatchResult> match(String path) {
        Matcher matcher = pattern.matcher(path);

        if(!matcher.matches())
            return Optional.empty();

        int count = matcher.groupCount();

        Map<String,String> parameters = new HashMap<>();

        for (int i = 0; i < variables.size(); i++) {

            parameters.put(variables.get(i), matcher.group(variableGroupStartFrom + i));
        }


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
                return parameters;
            }

            @Override
            public int compareTo(MatchResult o) {
                return 0;
            }
        };

        return Optional.of(result);
    }
}
