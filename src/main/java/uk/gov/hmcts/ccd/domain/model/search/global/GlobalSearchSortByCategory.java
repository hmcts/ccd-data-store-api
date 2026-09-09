package uk.gov.hmcts.ccd.domain.model.search.global;

import lombok.Getter;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields;

import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchSortService.KEYWORD_SUFFIX;

@Getter
public enum GlobalSearchSortByCategory {

    CASE_NAME(
        "caseName",
        GlobalSearchFields.CaseDataPaths.CASE_NAME_HMCTS_INTERNAL + KEYWORD_SUFFIX
    ),
    CASE_MANAGEMENT_CATEGORY_NAME(
        "caseManagementCategoryName",
        GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY_NAME + KEYWORD_SUFFIX
    ),
    NEXT_HEARING_DATE(
        "nextHearingDate",
        GlobalSearchFields.CaseDataPaths.NEXT_HEARING_DATE,
        "_last"
    ),
    CREATED_DATE(
        "createdDate",
        GlobalSearchFields.CREATED_DATE
    );

    private final String categoryName;
    private final String field;
    private final String missingValue;

    GlobalSearchSortByCategory(String categoryName, String field) {
        this(categoryName, field, null);
    }

    GlobalSearchSortByCategory(String categoryName, String field, String missingValue) {
        this.categoryName = categoryName;
        this.field = field;
        this.missingValue = missingValue;
    }

    public static GlobalSearchSortByCategory getEnum(String categoryName) {
        for (GlobalSearchSortByCategory value : GlobalSearchSortByCategory.values()) {
            if (categoryName.equalsIgnoreCase(value.categoryName)) {
                return value;
            }
        }

        return null;
    }

}
