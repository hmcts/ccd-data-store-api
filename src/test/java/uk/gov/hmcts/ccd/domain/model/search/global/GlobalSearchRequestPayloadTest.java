package uk.gov.hmcts.ccd.domain.model.search.global;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertAll;

public class GlobalSearchRequestPayloadTest {

    private List<String> validFields;

    @Before
    public void setUp() {
        validFields = List.of("ValidEntry", "ValidEntryTwo");
    }

    @Test
    public void shouldReplaceNullFieldsWithDefaults() {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);
        payload.setSearchCriteria(searchCriteria);

        payload.setDefaults();

        assertAll(
            () -> assertThat(payload.getSearchCriteria(), equalTo(searchCriteria)),
            () -> assertThat(payload.getSortCriteria().get(0).getSortBy(),
                equalTo(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName())),
            () -> assertThat(payload.getSortCriteria().get(0).getSortDirection(),
                equalTo(GlobalSearchSortDirection.ASCENDING.name())),
            () -> assertThat(payload.getMaxReturnRecordCount(), equalTo(25)),
            () -> assertThat(payload.getStartRecordNumber(), equalTo(1))
        );
    }

    @Test
    public void shouldReplaceOnlyNullSortCriteriaWithDefault() {

        GlobalSearchRequestPayload payload = new GlobalSearchRequestPayload();
        payload.setMaxReturnRecordCount(100);
        payload.setStartRecordNumber(1);
        SearchCriteria searchCriteria = new SearchCriteria();
        searchCriteria.setCaseManagementBaseLocationIds(validFields);
        searchCriteria.setCaseManagementRegionIds(validFields);
        payload.setSearchCriteria(searchCriteria);
        SortCriteria sortCriteriaOne = new SortCriteria();
        sortCriteriaOne.setSortBy(GlobalSearchSortByCategory.CASE_NAME.getCategoryName());
        SortCriteria sortCriteriaTwo = new SortCriteria();
        sortCriteriaTwo.setSortDirection(GlobalSearchSortDirection.DESCENDING.name());
        List<SortCriteria> sortCriteriaList = List.of(sortCriteriaOne, sortCriteriaTwo);
        payload.setSortCriteria(sortCriteriaList);

        payload.setDefaults();

        assertAll(
            () -> assertThat(payload.getSearchCriteria(), equalTo(searchCriteria)),
            () -> assertThat(payload.getSortCriteria().get(1).getSortBy(),
                equalTo(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName())),
            () -> assertThat(payload.getSortCriteria().get(1).getSortDirection(),
                equalTo(GlobalSearchSortDirection.DESCENDING.name())),
            () -> assertThat(payload.getSortCriteria().get(0).getSortDirection(),
                equalTo(GlobalSearchSortDirection.ASCENDING.name())),
            () -> assertThat(payload.getSortCriteria().get(0).getSortBy(),
                equalTo(GlobalSearchSortByCategory.CASE_NAME.getCategoryName())),
            () -> assertThat(payload.getMaxReturnRecordCount(), equalTo(100)),
            () -> assertThat(payload.getStartRecordNumber(), equalTo(1))
        );
    }
}
