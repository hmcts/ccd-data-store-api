package uk.gov.hmcts.ccd.domain.model.search.global;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GlobalSearchSortByCategoryTest {

    @DisplayName("getEnum should return correct enum for a valid CategoryName")
    @Test
    void shouldGetEnumForValidCategoryName() {

        // ARRANGE / ACT / ASSERT
        assertAll(
            () -> assertEquals(
                GlobalSearchSortByCategory.CASE_NAME,
                GlobalSearchSortByCategory.getEnum(GlobalSearchSortByCategory.CASE_NAME.getCategoryName())
            ),
            () -> assertEquals(
                GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME,
                GlobalSearchSortByCategory.getEnum(
                    GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME.getCategoryName()
                )
            ),
            () -> assertEquals(
                GlobalSearchSortByCategory.CREATED_DATE,
                GlobalSearchSortByCategory.getEnum(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName())
            )
        );
    }

    @DisplayName("getEnum should return null for an invalid CategoryName")
    @Test
    void shouldReturnNullForInvalidCategoryName() {

        // ARRANGE / ACT / ASSERT
        assertNull(GlobalSearchSortByCategory.getEnum("BAD_VALUE"));

    }

}
