package uk.gov.hmcts.ccd.domain.model.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_DELETE;
import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterCollectionOptions.ALLOW_INSERT;

public class DisplayContextParameterUtil {

    public static final String COLLECTION = "COLLECTION";

    public static String updateCollectionDisplayContextParameter(final String currentDisplayContextParameter,
                                                                 final List<String> values) {
        List<String> uniqueValues = new ArrayList<>(new HashSet<>(values));
        if (currentDisplayContextParameter == null) {
            return "#" + prepareCollectionParameter(uniqueValues);
        }

        List<String> parameters = extractParameters(currentDisplayContextParameter);

        if (parameters.stream().noneMatch(e -> e.contains(COLLECTION))) {
            parameters.add(prepareCollectionParameter(emptyList()));
        }

        return parameters.stream()
            .map(parameter -> updateIfCollection(parameter, uniqueValues))
            .map(e -> "#" + e)
            .collect(Collectors.joining(","));
    }

    private static List<String> extractParameters(String currentDisplayContextParameter) {
        String[] split = currentDisplayContextParameter.split("#", -1);
        return removeEmptiesAndDropComa(split);
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

            return prepareCollectionParameter(collect);
        }
        return parameter;
    }

    private static List<String> extractCollectionParameterValues(String displayContextParameter) {

        String prefix = COLLECTION + "(";
        int start = displayContextParameter.indexOf(prefix);
        String s1 = displayContextParameter.substring(start + prefix.length());
        String s2 = s1.substring(0, s1.indexOf(")"));

        return isBlank(s2) ? emptyList() : Arrays.stream(s2.split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    }

    private static String prepareCollectionParameter(List<String> values) {
        Collections.sort(values);
        return COLLECTION + "(" + String.join(",", values) + ")";
    }
}
