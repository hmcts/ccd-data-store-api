package uk.gov.hmcts.ccd.data.casedetails.search;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData.CaseField;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.lang.String.format;

@Component
public class SortOrderQueryBuilder {

    private static final String DATA_FIELD = "data";
    private static final String CREATED_DATE = "created_date";
    private static final String SPACE = " ";
    private static final String COMMA = ",";
    private static final String COLON = ":";
    private static final String SORT_ORDER_FIELD = "sortOrderField%s";

    public String buildSortOrderClause(MetaData metaData, final Map<String, Object> parametersToBind) {
        StringBuilder sb = new StringBuilder();
        List<SortOrderField> sortOrderFields = metaData.getSortOrderFields();
        IntStream.range(0, sortOrderFields.size()).forEach(idx -> {
            SortOrderField sortOrderField = sortOrderFields.get(idx);
            String sortOrderFieldIdx = format(SORT_ORDER_FIELD, idx);
            if (sortOrderField.isMetadata()) {
                parametersToBind.put(sortOrderFieldIdx, getMataFieldName(sortOrderField.getCaseFieldId()));
                sb.append(COLON + sortOrderFieldIdx);
            } else {
                parametersToBind.put(sortOrderFieldIdx, convertFieldNameToJSONBsqlFormat(sortOrderField.getCaseFieldId()));
                sb.append(COLON + sortOrderFieldIdx);
            }
            sb.append(SPACE);
            sb.append(sortOrderField.getDirection());
            sb.append(COMMA);
            sb.append(SPACE);
        });
        // always sort with creation_date as a last order so that it supports cases where no values at all for the configured fields and also default fallback.
        return sb.append(CREATED_DATE + SPACE + SortDirection.fromOptionalString(metaData.getSortDirection())).toString();
    }

    private String getMataFieldName(String fieldName) {
        String metaFieldName = fieldName.startsWith("[") ? StringUtils.substringBetween(fieldName, "[", "]") : fieldName;
        return CaseField.valueOf(metaFieldName).getDbColumnName();
    }

    private static String convertFieldNameToJSONBsqlFormat(final String in) {
        return DATA_FIELD + " #>> '{" + StringUtils.replace(in, ".", ",") + "}'";
    }

}
