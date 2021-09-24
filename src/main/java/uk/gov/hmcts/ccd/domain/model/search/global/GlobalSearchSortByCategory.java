package uk.gov.hmcts.ccd.domain.model.search.global;

import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields;

import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchSortService.KEYWORD_SUFFIX;

public enum GlobalSearchSortByCategory {

    CASE_NAME(
        "caseName",
        GlobalSearchFields.CaseDataPaths.CASE_NAME_HMCTS_INTERNAL + KEYWORD_SUFFIX
    ),
    CASE_MANAGEMENT_CATEGORY_NAME(
        "caseManagementCategoryName",
        GlobalSearchFields.CaseDataPaths.CASE_MANAGEMENT_CATEGORY_NAME + KEYWORD_SUFFIX
    ),
    CREATED_DATE(
        "createdDate",
        GlobalSearchFields.CREATED_DATE
    );

    private final String categoryName;
    private final String field;

    GlobalSearchSortByCategory(String categoryName, String field) {
        this.categoryName = categoryName;
        this.field = field;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getField() {
        return field;
    }

    public static GlobalSearchSortByCategory getEnum(String categoryName) {
        for (GlobalSearchSortByCategory value: GlobalSearchSortByCategory.values()) {
            if (categoryName.equalsIgnoreCase(value.categoryName)) {
                return value;
            }
        }

        return null;
    }

}
