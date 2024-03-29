package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.data.casedetails.search.SortDirection.fromOptionalString;

@Component
public class SortOrderQueryBuilder {

    private static final String DATA_FIELD = "data";
    private static final String CREATED_DATE = "created_date";
    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String CASE_FIELD_ID_PATTERN = "^['a-zA-Z0-9\\[\\]\\#%\\&()\\.?_\\£\\s\\xA0-]+$";


    public String buildSortOrderClause(MetaData metaData) {
        StringBuilder sb = new StringBuilder();
        metaData.getSortOrderFields().forEach(sortOrderField -> {
            String caseFieldId = sortOrderField.getCaseFieldId();
            if (!caseFieldId.matches(CASE_FIELD_ID_PATTERN)) {
                throw new BadRequestException("Sort order field is invalid.");
            }
            if (sortOrderField.isMetadata()) {
                sb.append(getMataFieldName(caseFieldId));
            } else {
                sb.append(convertFieldNameToJsonbSqlFormat(caseFieldId));
            }
            sb.append(SPACE);
            sb.append(fromOptionalString(ofNullable(sortOrderField.getDirection())));
            sb.append(COMMA);
            sb.append(SPACE);
        });
        // always sort with creation_date as a last order so that it supports cases where
        // no values at all for the configured fields and also default fallback.
        return sb.append(CREATED_DATE + SPACE + fromOptionalString(metaData.getSortDirection())).toString();
    }

    private String getMataFieldName(String fieldName) {
        String metaFieldName = fieldName.startsWith("[")
            ? StringUtils.substringBetween(fieldName, "[", "]") : fieldName;
        return CaseField.valueOf(metaFieldName).getDbColumnName();
    }

    private static String convertFieldNameToJsonbSqlFormat(final String in) {
        return DATA_FIELD + " #>> '{" + StringUtils.replace(in, ".", ",") + "}'";
    }

}
