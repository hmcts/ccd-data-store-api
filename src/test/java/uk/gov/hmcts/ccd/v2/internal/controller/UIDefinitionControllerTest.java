package uk.gov.hmcts.ccd.v2.internal.controller;

import java.util.List;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.UIBannerResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UISearchInputsResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UIWorkbasketInputsResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.BannerBuilder.newBanner;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

@DisplayName("UIDefinitionController")
class UIDefinitionControllerTest {
    private static final String CASE_TYPE_ID = "caseTypeId";

    private WorkbasketInput workbasketInput1 = aWorkbasketInput().withFieldId("field1").build();
    private WorkbasketInput workbasketInput2 = aWorkbasketInput().withFieldId("field2").build();
    private SearchInput searchInput1 = aSearchInput().withFieldId("field1").build();
    private SearchInput searchInput2 = aSearchInput().withFieldId("field2").build();

    private Banner banner1 = newBanner().withBannerEnabled(true)
                                        .withBannerDescription("Test Description1")
                                        .withBannerUrlText("Click here to see it.>>>")
                                        .withBannerUrl("http://localhost:3451/test").build();

    private Banner banner2 = newBanner().withBannerEnabled(true)
                                        .withBannerDescription("Test Description2")
                                        .withBannerUrlText("Click here to see it.>>>")
                                        .withBannerUrl("http://localhost:3451/test").build();

    private final List<WorkbasketInput> workbasketInputs = Lists.newArrayList(workbasketInput1, workbasketInput2);
    private final List<SearchInput> searchInputs = Lists.newArrayList(searchInput1, searchInput2);
    private final List<Banner> banners = Lists.newArrayList(banner1, banner2);
    private final List<String> jurisdictionReferenes = Lists.newArrayList("TEST", "FAMILY LAW");

    @Mock
    private GetCriteriaOperation getCriteriaOperation;

    @Mock
    private GetBannerOperation getBannerOperation;

    @InjectMocks
    private UIDefinitionController uiDefinitionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(workbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ID, CAN_READ, WORKBASKET);
        doReturn(searchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ID, CAN_READ, SEARCH);
        doReturn(banners).when(getBannerOperation).execute(jurisdictionReferenes);
    }

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/work-basket-inputs")
    class GetWorkbasketInputsDetails {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<UIWorkbasketInputsResource> response = uiDefinitionController.getWorkbasketInputsDetails(CASE_TYPE_ID);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    UIWorkbasketInputsResource.UIWorkbasketInput[] workbasketInputs = response.getBody().getWorkbasketInputs();
                    assertThat(Lists.newArrayList(workbasketInputs), hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                        hasProperty("field", hasProperty("id", is("field2")))));
                }
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getCriteriaOperation.execute(CASE_TYPE_ID, CAN_READ, WORKBASKET)).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class,
                () -> uiDefinitionController.getWorkbasketInputsDetails(CASE_TYPE_ID));
        }
    }

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/search-inputs")
    class GetSearchInputsDetails {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<UISearchInputsResource> response = uiDefinitionController.getSearchInputsDetails(CASE_TYPE_ID);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    UISearchInputsResource.UISearchInput[] searchInputs = response.getBody().getSearchInputs();
                    assertThat(Lists.newArrayList(searchInputs), hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                                          hasProperty("field", hasProperty("id", is("field2")))));
                }
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getCriteriaOperation.execute(CASE_TYPE_ID, CAN_READ, SEARCH)).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class,
                         () -> uiDefinitionController.getSearchInputsDetails(CASE_TYPE_ID));
        }
    }

    @Nested
    @DisplayName("GET /internal/banners")
    class GetBanners {

        @Test
        @DisplayName("should return 200 when banners found")
        void caseFound() {
            final ResponseEntity<UIBannerResource> response = uiDefinitionController.getBanners(Optional.of(jurisdictionReferenes));

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    UIBannerResource bannerResource = response.getBody();
                    assertThat(Lists.newArrayList(bannerResource.getBanners()), hasItems(hasProperty("bannerDescription", is("Test Description1")),
                        hasProperty("bannerDescription", is("Test Description2"))));
                }
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getBannerOperation.execute(jurisdictionReferenes)).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class,
                () -> uiDefinitionController.getBanners(Optional.of(jurisdictionReferenes)));
        }
    }
}
