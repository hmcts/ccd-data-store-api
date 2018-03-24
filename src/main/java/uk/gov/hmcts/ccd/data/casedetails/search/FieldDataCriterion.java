package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class FieldDataCriterion extends Criterion {

    private static final String DATA_FIELD = "data";

    private static final String FIELD_FILTER_PREFIX = "case";

    public FieldDataCriterion(String field, String soughtValue) {
        super(field, soughtValue);
    }

    @Override
    public String buildClauseString(int position, String operation) {
        return convertFieldName(this.getField()) + operation + makeCaseInsensitive(POSITION_PREFIX + position);
    }

    private String convertFieldName(String field) {
        return Optional.of(field)
            .map(this::removeFieldNameFilterPrefix)
            .map(this::convertFieldNameToJSONBsqlFormat)
            .map(this::makeCaseInsensitive)
            .orElseThrow(() -> new IllegalArgumentException("Field not found"));
    }

    private String convertFieldNameToJSONBsqlFormat(final String in) {
        return DATA_FIELD + " #>> '{" + StringUtils.replace(in, ".", ",") + "}'";
    }

    private String removeFieldNameFilterPrefix(final String in) {
        return StringUtils.removeStart(in, FIELD_FILTER_PREFIX + Criterion.TOKEN_SEPARATOR);
    }

}
