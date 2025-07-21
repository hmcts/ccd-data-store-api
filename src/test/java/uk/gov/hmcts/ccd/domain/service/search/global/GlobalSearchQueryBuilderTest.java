package uk.gov.hmcts.ccd.domain.service.search.global;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortByCategory;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchSortDirection;
import uk.gov.hmcts.ccd.domain.model.search.global.SortCriteria;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@ExtendWith(MockitoExtension.class)
class GlobalSearchQueryBuilderTest {

    @InjectMocks
    private GlobalSearchQueryBuilder classUnderTest;

    @Nested
    @DisplayName("GlobalSearch Sort")
    class GlobalSearchSort {

        @DisplayName("Null Check: should return empty sort list when supplied with null request")
        @Test
        void shouldReturnEmptySortForNullRequest() {
            List<SortOptions> output = classUnderTest.globalSearchSort(null);
            assertThat(output).isEmpty();
        }

        @DisplayName("Null Check: should return empty sort list when supplied with null SortCriteria list")
        @Test
        void shouldReturnEmptySortWhenNullSortCriteriaList() {
            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(null);
            List<SortOptions> output = classUnderTest.globalSearchSort(request);
            assertThat(output).isEmpty();
        }

        @DisplayName("Empty Check: should return empty sort list when supplied with empty SortCriteria list")
        @Test
        void shouldReturnEmptySortWhenEmptySortCriteriaList() {
            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(List.of());
            List<SortOptions> output = classUnderTest.globalSearchSort(request);
            assertThat(output).isEmpty();
        }

        @DisplayName("Empty Criteria Check: should return empty sort list when SortCriteria values are null or empty")
        @Test
        void shouldReturnEmptySortWhenSortCriteriaValuesAreNullOrEmpty() {
            SortCriteria sortCriteriaEmpty = new SortCriteria();
            sortCriteriaEmpty.setSortBy("");
            sortCriteriaEmpty.setSortDirection("");

            SortCriteria sortCriteriaNull = new SortCriteria();
            sortCriteriaNull.setSortBy(null);
            sortCriteriaNull.setSortDirection(null);

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(Arrays.asList(sortCriteriaEmpty, sortCriteriaNull, null));
            List<SortOptions> output = classUnderTest.globalSearchSort(request);
            assertThat(output).isEmpty();
        }

        @DisplayName("Bad Criteria Check: should return empty sort list when supplied with Bad SortCriteria")
        @Test
        void shouldReturnEmptySortWhenBadSortCriteria() {
            SortCriteria sortCriteriaBad = new SortCriteria();
            sortCriteriaBad.setSortBy("BAD_VALUE");
            sortCriteriaBad.setSortDirection("BAD_VALUE");

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(List.of(sortCriteriaBad));
            List<SortOptions> output = classUnderTest.globalSearchSort(request);
            assertThat(output).isEmpty();
        }

        @DisplayName("Many Sort Criteria: should return sort list when supplied with valid SortCriteria")
        @Test
        void shouldReturnManySortForManySortCriteria() {
            SortCriteria sortCriteria1 = new SortCriteria();
            sortCriteria1.setSortBy(GlobalSearchSortByCategory.CASE_NAME.getCategoryName());
            sortCriteria1.setSortDirection(GlobalSearchSortDirection.ASCENDING.name());

            SortCriteria sortCriteria2 = new SortCriteria();
            sortCriteria2.setSortBy(GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME.getCategoryName());
            sortCriteria2.setSortDirection(GlobalSearchSortDirection.DESCENDING.name());

            SortCriteria sortCriteria3 = new SortCriteria();
            sortCriteria3.setSortBy(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName());
            sortCriteria3.setSortDirection(null);

            GlobalSearchRequestPayload request = new GlobalSearchRequestPayload();
            request.setSortCriteria(List.of(sortCriteria1, sortCriteria2, sortCriteria3));
            List<SortOptions> output = classUnderTest.globalSearchSort(request);

            assertThat(output.size()).isEqualTo(3);
            assertAll(
                () -> assertSortCriteria(GlobalSearchSortByCategory.CASE_NAME.getField(), SortOrder.Asc, output.get(0)),

                () -> assertSortCriteria(GlobalSearchSortByCategory.CASE_MANAGEMENT_CATEGORY_NAME.getField(),
                    SortOrder.Desc, output.get(1)),
                () -> assertSortCriteria(GlobalSearchSortByCategory.CREATED_DATE.getField(), SortOrder.Asc,
                    output.get(2))
            );
        }

        private void assertSortCriteria(String expectedField, SortOrder expectedOrder, SortOptions sortOption) {
            assertThat(sortOption).isNotNull();
            assertThat(sortOption.field().field()).isEqualTo(expectedField);
            assertThat(sortOption.field().order()).isEqualTo(expectedOrder);
        }
    }
}
