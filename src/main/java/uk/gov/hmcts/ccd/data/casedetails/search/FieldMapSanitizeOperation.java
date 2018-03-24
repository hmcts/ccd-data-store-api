package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;

import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

@Named
@Singleton
public class FieldMapSanitizeOperation {

    private static final String VALID_FIELD_NAME_REGEX = "^[A-Za-z0-9_.-]+$";
    private static final String CASE_FIELD_PREFIX = "case.";

    public Map<String, String> execute(final Map<String, String> params) {
        checkFieldNames(params.keySet());
        return params.entrySet()
                .stream()
                .filter(e -> isCaseFieldParameter(e.getKey()))
                .map(this::remmoveCaseFieldPrefix)
                .collect(Collectors.toMap(Entry::getKey, e -> StringUtils.trim(e.getValue())));
    }

    private void checkFieldNames(final Set<String> keys) {
        final boolean allFieldNamesOK = keys.stream()
                .allMatch(key -> key.matches(VALID_FIELD_NAME_REGEX));
        if (!allFieldNamesOK) {
            throw new BadRequestException("Field Names Invalid");
        }
    }

    private Entry<String, String> remmoveCaseFieldPrefix(Entry<String, String> entry) {
        return new AbstractMap.SimpleEntry<>(
                entry.getKey().replaceFirst(CASE_FIELD_PREFIX, ""),
                entry.getValue());

    }

    public static boolean isCaseFieldParameter(String s) {
        return s.startsWith(CASE_FIELD_PREFIX);
    }

}
