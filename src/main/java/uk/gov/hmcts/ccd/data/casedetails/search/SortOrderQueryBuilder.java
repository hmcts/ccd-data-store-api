package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation.VALID_FIELD_NAME_REGEX;

@Component
public class SortOrderQueryBuilder {

    private static final String DATA_FIELD = "data";
    private static final String CREATED_DATE = "created_date";
    private static final String SPACE = " ";
    private static final String COMMA = ",";

    private static final Set<String> VALID_COLUMNS_FOR_ORDER_BY
        = Collections.unmodifiableSet((Set<? extends String>) Stream
                                          .of(CaseDetailsEntity.CREATED_DATE_FIELD_COL,
                                              CaseDetailsEntity.LAST_MODIFIED_FIELD_COL,
                                              CaseDetailsEntity.JURISDICTION_FIELD_COL,
                                              CaseDetailsEntity.CASE_TYPE_ID_FIELD_COL,
                                              CaseDetailsEntity.STATE_FIELD_COL,
                                              CaseDetailsEntity.REFERENCE_FIELD_COL,
                                              CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL)
                                          .collect(Collectors.toCollection(HashSet::new)));

    public String buildSortOrderClause(MetaData metaData) {
        StringBuilder sb = new StringBuilder();
        metaData.getSortOrderFields().forEach(sortOrderField -> {
            if (sortOrderField.isMetadata() && VALID_COLUMNS_FOR_ORDER_BY.contains(getMataFieldName(sortOrderField.getCaseFieldId()))) {
                sb.append(getMataFieldName(sortOrderField.getCaseFieldId()));
                appendOrder(sb, sortOrderField);
            } else if (sortOrderField.getCaseFieldId().matches(VALID_FIELD_NAME_REGEX)) {
                sb.append(convertFieldNameToJSONBsqlFormat(sortOrderField.getCaseFieldId()));
                appendOrder(sb, sortOrderField);
            }
        });
        // always sort with creation_date as a last order so that it supports cases where no values at all for the configured fields and also default fallback.
        return sb.append(CREATED_DATE + SPACE + SortDirection.fromOptionalString(metaData.getSortDirection())).toString();
    }

    private void appendOrder(final StringBuilder sb, final SortOrderField sortOrderField) {
        sb.append(SPACE);
        sb.append(sortOrderField.getDirection());
        sb.append(COMMA);
        sb.append(SPACE);
    }

    private String getMataFieldName(String fieldName) {
        String metaFieldName = fieldName.startsWith("[") ? StringUtils.substringBetween(fieldName, "[", "]") : fieldName;
        return CaseField.valueOf(metaFieldName).getDbColumnName();
    }

    private static String convertFieldNameToJSONBsqlFormat(final String in) {
        return DATA_FIELD + " #>> '{" + StringUtils.replace(in, ".", ",") + "}'";
    }

}
