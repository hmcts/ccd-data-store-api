package uk.gov.hmcts.ccd.domain.model.search.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.assertj.core.api.Assertions.assertThat;

class GlobalSearchSortByCategoryTest {

    @DisplayName("getEnum should return correct enum for a valid CategoryName")
    @Test
    void shouldGetEnumForValidCategoryName() {

        // ARRANGE / ACT / ASSERT
        assertAll(
            () -> assertThat(GlobalSearchSortByCategory.getEnum(GlobalSearchSortByCategory.CASE_NAME.getCategoryName()))
                .isEqualTo(GlobalSearchSortByCategory.CASE_NAME),
            () -> assertThat(GlobalSearchSortByCategory.getEnum(
                GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME.getCategoryName()))
                .isEqualTo(GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME),
            () -> assertThat(
                GlobalSearchSortByCategory.getEnum(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName()))
                .isEqualTo(GlobalSearchSortByCategory.CREATED_DATE),
            () -> assertThat(
                GlobalSearchSortByCategory.getEnum(GlobalSearchSortByCategory.NEXT_HEARING_DATE.getCategoryName()))
                .isEqualTo(GlobalSearchSortByCategory.NEXT_HEARING_DATE)

        );
    }

    @DisplayName("getEnum should return null for an invalid CategoryName")
    @Test
    void shouldReturnNullForInvalidCategoryName() {

        // ARRANGE / ACT / ASSERT
        assertThat(GlobalSearchSortByCategory.getEnum("BAD_VALUE")).isNull();

    }

}
