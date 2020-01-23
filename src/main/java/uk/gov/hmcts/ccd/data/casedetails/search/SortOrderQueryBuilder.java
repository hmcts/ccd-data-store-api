package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.data.casedetails.search.SortDirection.fromOptionalString;

@Component
public class SortOrderQueryBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(SortOrderQueryBuilder.class);

    private static final String DATA_FIELD = "data";
    private static final String CREATED_DATE = "created_date";
    private static final String SPACE = " ";
    private static final String SPECIAL_CHARS_REGEXP = "[\\s\\\\\\/,;\\)\\('\"`]";
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile(SPECIAL_CHARS_REGEXP);
    private static final String COMMA = ",";

    public String buildSortOrderClause(MetaData metaData) {
        StringBuilder sb = new StringBuilder();
        metaData.getSortOrderFields().forEach(sortOrderField -> {
            Matcher matcher = SPECIAL_CHARS_PATTERN.matcher(sortOrderField.getCaseFieldId());
            if (matcher.find()) {
                LOG.error("Illegal sort order field id: {}", sortOrderField.getCaseFieldId());
                throw new IllegalArgumentException("Illegal sortOrderField.caseFieldId=" + sortOrderField.getCaseFieldId());
            } else {
                if (sortOrderField.isMetadata()) {
                    sb.append(getMataFieldName(sortOrderField.getCaseFieldId()));
                } else {
                    sb.append(convertFieldNameToJSONBsqlFormat(sortOrderField.getCaseFieldId()));
                }
                sb.append(SPACE);
                sb.append(fromOptionalString(ofNullable(sortOrderField.getDirection())));
                sb.append(COMMA);
                sb.append(SPACE);
            }
        });
        // always sort with creation_date as a last order so that it supports cases where no values at all for the configured fields and also default fallback.
        return sb.append(CREATED_DATE + SPACE + fromOptionalString(metaData.getSortDirection())).toString();
    }

    private String getMataFieldName(String fieldName) {
        String metaFieldName = fieldName.startsWith("[") ? StringUtils.substringBetween(fieldName, "[", "]") : fieldName;
        return CaseField.valueOf(metaFieldName).getDbColumnName();
    }

    private static String convertFieldNameToJSONBsqlFormat(final String in) {
        return DATA_FIELD + " #>> '{" + StringUtils.replace(in, ".", ",") + "}'";
    }

}
