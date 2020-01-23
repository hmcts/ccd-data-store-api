package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SortOrderQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SortOrderQueryBuilder.class);

    private static final String DATA_FIELD = "data";
    private static final String CREATED_DATE = "created_date";
    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String SPECIAL_CHARS_REGEXP = "[\\s\\\\\\/,;\\)\\('\"`]";
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile(SPECIAL_CHARS_REGEXP);

    public String buildSortOrderClause(MetaData metaData) {
        StringBuilder sb = new StringBuilder();
        metaData.getSortOrderFields().forEach(sortOrderField -> {
            Matcher matcher = SPECIAL_CHARS_PATTERN.matcher(sortOrderField.getCaseFieldId());
            if (!matcher.find()) {
                if (sortOrderField.isMetadata()) {
                    sb.append(getMataFieldName(sortOrderField.getCaseFieldId()));
                } else {
                    sb.append(convertFieldNameToJSONBsqlFormat(sortOrderField.getCaseFieldId()));
                }
                sb.append(SPACE);
                sb.append(sortOrderField.getDirection());
                sb.append(COMMA);
                sb.append(SPACE);
            }
        });
        // always sort with creation_date as a last order so that it supports cases where no values at all for the configured fields and also default fallback.
        return sb.append(CREATED_DATE + SPACE + SortDirection.fromOptionalString(metaData.getSortDirection())).toString();
    }

    private String getMataFieldName(String fieldName) {
        String metaFieldName = fieldName.startsWith("[") ? StringUtils.substringBetween(fieldName, "[", "]") : fieldName;
        String dbColumnName = null;
        try {
            dbColumnName = CaseField.valueOf(metaFieldName).getDbColumnName();
        } catch (IllegalArgumentException iae) {
            LOG.warn("Illegal meta field name: {}, exception: {}", fieldName, iae);
        }
        return dbColumnName;
    }

    private static String convertFieldNameToJSONBsqlFormat(final String in) {
        return DATA_FIELD + " #>> '{" + StringUtils.replace(in, ".", ",") + "}'";
    }

}
