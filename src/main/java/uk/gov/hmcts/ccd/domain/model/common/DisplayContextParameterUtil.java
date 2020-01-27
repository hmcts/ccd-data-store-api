package uk.gov.hmcts.ccd.domain.model.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_DELETE;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_INSERT;

public class DisplayContextParameterUtil {

    public static String COLLECTION = "COLLECTION";

    public static String updateCollectionDisplayContextParameter(final String currentDisplayContextParameter,
                                                                 final List<String> values) {

        List<String> parameters;
        if (currentDisplayContextParameter != null) {
            String[] split = currentDisplayContextParameter.split("#", -1);
            List<String> collect = removeEmptiesAndDropComa(split);

            if (collect.stream().noneMatch(e -> e.contains(COLLECTION))) {
                collect.add(COLLECTION + "()");
            }

            parameters = collect.stream()
                .map(e -> updateIfCollection(e, values))
                .map(e -> "#" + e)
                .collect(Collectors.toList());
        } else {
            parameters = new ArrayList<>(values);
            Collections.sort(parameters);
            return "#" + COLLECTION + "(" + String.join(",", parameters) + ")";
        }

        return String.join(",", parameters);
    }

    private static List<String> removeEmptiesAndDropComa(String[] split) {
        return Arrays.stream(split)
                    .filter(e -> e.length() > 0)
                    .map(e -> e.endsWith(",") ? e.substring(0, e.length() - 1) : e)
                    .collect(Collectors.toList());
    }

    private static String updateIfCollection(String parameter, List<String> values) {
        if (parameter.startsWith(COLLECTION)) {
            List<String> collectionParameterValues = extractCollectionParameterValues(parameter);
            List<String> collect = collectionParameterValues.stream()
                .filter(p -> !p.equals(ALLOW_INSERT.getOption()) && !p.equals(ALLOW_DELETE.getOption()))
                .collect(Collectors.toList());
            collect.addAll(values);
            Collections.sort(collect);

            return COLLECTION + "(" + String.join(",", collect) + ")";
        }
        return parameter;
    }

    private static List<String> extractCollectionParameterValues(String displayContextParameter) {

        String prefix = COLLECTION + "(";
        int start = displayContextParameter.indexOf(prefix);
        String s1 = displayContextParameter.substring(start + prefix.length());
        String s2 = s1.substring(0, s1.indexOf(")"));

        return isBlank(s2) ? Collections.emptyList() : Arrays.stream(s2.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    }
}
