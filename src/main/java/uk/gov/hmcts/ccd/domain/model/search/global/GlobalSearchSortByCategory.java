package uk.gov.hmcts.ccd.domain.model.search.global;

public enum GlobalSearchSortByCategory {
    CASE_NAME("caseName"),
    CASE_MANAGEMENT_CATEGORY_NAME("caseManagementCategoryName"),
    CREATED_DATE("createdDate");


    private final String categoryName;

    GlobalSearchSortByCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }
}
