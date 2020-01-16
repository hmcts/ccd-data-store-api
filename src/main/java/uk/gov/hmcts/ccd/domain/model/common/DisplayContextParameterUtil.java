package uk.gov.hmcts.ccd.domain.model.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class DisplayContextParameterUtil {
    public enum Parameter {
        COLLECTION("#COLLECTION"),
        TABLE("#TABLE");

        private final String description;

        Parameter(String description) {
            this.description = description;
        }

        public String getDescription() {
            return this.description;
        }
    }

    public static String updateDisplayContextParameter(String currentDisplayContextParameter,
                                                       Parameter parameter,
                                                       List<String> values) {
        Map<Parameter, List<String>> propertiesMap = new HashMap<>();

        for (Parameter param : Parameter.values()) {
            if (currentDisplayContextParameter != null && currentDisplayContextParameter.contains(param.getDescription())) {
                propertiesMap.put(param, extractParameterValues(param, currentDisplayContextParameter));
            }
        }
        propertiesMap.put(parameter, values);
        propertiesMap.forEach((k, v) -> Collections.sort(v));

        return propertiesMap.keySet().stream()
            .map(strings -> strings.getDescription() + "(" + String.join(",", propertiesMap.get(strings)) + ")")
            .sorted()
            .collect(joining(","));
    }

    private static List<String> extractParameterValues(Parameter param,
                                                       String displayContextParameter) {

        String prefix = param.getDescription() + "(";
        int start = displayContextParameter.indexOf(prefix);
        String s1 = displayContextParameter.substring(start + prefix.length());
        String s2 = s1.substring(0, s1.indexOf(")"));

        return Arrays.stream(s2.split(","))
            .map(String::trim)
            .sorted()
            .collect(Collectors.toList());
    }
}
