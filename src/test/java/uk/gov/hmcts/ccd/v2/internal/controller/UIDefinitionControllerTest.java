package uk.gov.hmcts.ccd.v2.internal.controller;

import java.util.List;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.definition.Banner;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionUiConfigDefinition;
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetBannerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.v2.internal.resource.BannerViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.JurisdictionViewResource;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetJurisdictionUiConfigOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.JurisdictionConfigViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.SearchInputsViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.WorkbasketInputsViewResource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.SEARCH;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.BannerBuilder.newBanner;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionUiConfigBuilder.newJurisdictionUiConfig;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.SearchInputBuilder.aSearchInput;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.WorkbasketInputBuilder.aWorkbasketInput;

@DisplayName("UIDefinitionController")
class UIDefinitionControllerTest {
    private static final String CASE_TYPE_ID = "caseTypeId";
    private static final String SHOW_CONDITION = "aShowCondition";

    private WorkbasketInput workbasketInput1 = aWorkbasketInput().withFieldId("field1").withShowCondition(SHOW_CONDITION).build();
    private WorkbasketInput workbasketInput2 = aWorkbasketInput().withFieldId("field2").build();
    private SearchInput searchInput1 = aSearchInput().withFieldId("field1").build();
    private SearchInput searchInput2 = aSearchInput().withFieldId("field2").withShowCondition(SHOW_CONDITION).build();

    private Banner banner1 = newBanner().withBannerEnabled(true)
                                        .withBannerDescription("Test Description1")
                                        .withBannerUrlText("Click here to see it.>>>")
                                        .withBannerUrl("http://localhost:3451/test").build();

    private Banner banner2 = newBanner().withBannerEnabled(true)
                                        .withBannerDescription("Test Description2")
                                        .withBannerUrlText("Click here to see it.>>>")
                                        .withBannerUrl("http://localhost:3451/test").build();

    private JurisdictionUiConfigDefinition jurisdictionUiConfigDefinition1 = newJurisdictionUiConfig()
                                                                    .withId("Reference 1")
                                                                    .withName("Name 1")
                                                                    .withShutteredEnabled(true)
                                                                    .build();
    private JurisdictionUiConfigDefinition jurisdictionUiConfigDefinition2 = newJurisdictionUiConfig()
                                                                    .withId("Reference 2")
                                                                    .withName("Name 2")
                                                                    .withShutteredEnabled(false)
                                                                    .build();

    private final List<WorkbasketInput> workbasketInputs = Lists.newArrayList(workbasketInput1, workbasketInput2);
    private final List<SearchInput> searchInputs = Lists.newArrayList(searchInput1, searchInput2);
    private final List<Banner> banners = Lists.newArrayList(banner1, banner2);
    private final List<JurisdictionUiConfigDefinition> jurisdictionUiConfigDefinitions =
        Lists.newArrayList(jurisdictionUiConfigDefinition1, jurisdictionUiConfigDefinition2);
    private final List<String> jurisdictionReferenes = Lists.newArrayList("TEST", "FAMILY LAW");

    @Mock
    private GetCriteriaOperation getCriteriaOperation;

    @Mock
    private GetBannerOperation getBannerOperation;

    @Mock
    private GetJurisdictionUiConfigOperation getJurisdictionUiConfigOperation;

    @Mock
    private GetUserProfileOperation getUserProfileOperation;

    @InjectMocks
    private UIDefinitionController uiDefinitionController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(workbasketInputs).when(getCriteriaOperation).execute(CASE_TYPE_ID, CAN_READ, WORKBASKET);
        doReturn(searchInputs).when(getCriteriaOperation).execute(CASE_TYPE_ID, CAN_READ, SEARCH);
        doReturn(banners).when(getBannerOperation).execute(jurisdictionReferenes);
        doReturn(jurisdictionUiConfigDefinitions).when(getJurisdictionUiConfigOperation).execute(jurisdictionReferenes);
    }

    @Nested
    @DisplayName("GET /internal/case-types/{caseTypeId}/work-basket-inputs")
    class GetWorkbasketInputsDetails {

        @Test
        @DisplayName("should return 200 when case found")
        void caseFound() {
            final ResponseEntity<WorkbasketInputsViewResource> response = uiDefinitionController.getWorkbasketInputsDetails(CASE_TYPE_ID);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    WorkbasketInputsViewResource.WorkbasketInputView[] workbasketInputs = response.getBody().getWorkbasketInputs();
                    assertThat(Lists.newArrayList(workbasketInputs), hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                        hasProperty("field", hasProperty("id", is("field2"))),
                            hasProperty("field", hasProperty("showCondition", is(SHOW_CONDITION)))));
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
            final ResponseEntity<SearchInputsViewResource> response = uiDefinitionController.getSearchInputsDetails(CASE_TYPE_ID);

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    SearchInputsViewResource.SearchInputView[] searchInputs = response.getBody().getSearchInputs();
                    assertThat(Lists.newArrayList(searchInputs), hasItems(hasProperty("field", hasProperty("id", is("field1"))),
                                                                          hasProperty("field", hasProperty("id", is("field2"))),
                                                                          hasProperty("field", hasProperty("showCondition", is(SHOW_CONDITION)))));
                }
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getCriteriaOperation.execute(CASE_TYPE_ID, CAN_READ, SEARCH)).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class, () -> uiDefinitionController.getSearchInputsDetails(CASE_TYPE_ID));
        }
    }

    @Nested
    @DisplayName("GET /internal/banners")
    class GetBanners {

        @Test
        @DisplayName("should return 200 when banners found")
        void bannerFound() {
            final ResponseEntity<BannerViewResource> response = uiDefinitionController.getBanners(Optional.of(jurisdictionReferenes));

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    BannerViewResource bannerResource = response.getBody();
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

        @Test
        @DisplayName("should return empty list of banners")
        void shouldReturnEmptyBannersList() {
            ResponseEntity<BannerViewResource>  responseEntity = uiDefinitionController.getBanners(Optional.empty());
            assertEquals(0, responseEntity.getBody().getBanners().size());
        }

        @Test
        @DisplayName("should return banners")
        void shouldGetBanners() {
            List<Banner> bannersReturned = getBannerOperation.execute(jurisdictionReferenes);

            assertEquals(2, bannersReturned.size());
            assertEquals("Test Description1", bannersReturned.get(0).getBannerDescription());
            assertEquals("Click here to see it.>>>", bannersReturned.get(0).getBannerUrlText());
            assertEquals("http://localhost:3451/test", bannersReturned.get(0).getBannerUrl());
            assertEquals(true, bannersReturned.get(0).getBannerEnabled());

            assertEquals("Test Description2", bannersReturned.get(1).getBannerDescription());
            assertEquals("Click here to see it.>>>", bannersReturned.get(1).getBannerUrlText());
            assertEquals("http://localhost:3451/test", bannersReturned.get(1).getBannerUrl());
            assertEquals(true, bannersReturned.get(1).getBannerEnabled());
        }
    }

    @Nested
    @DisplayName("GET /internal/jurisdictions")
    class GetJurisdictions {

        @Test
        @DisplayName("should throw exception when access type not found")
        void accessTypeNotExists() {
            assertThrows(BadRequestException.class,
                () -> uiDefinitionController.getJurisdictions("access_not_exists"));
        }

        @Test
        @DisplayName("should throw exception when jurisdictions not found")
        void shouldThrowExceptionWhenJurisdictionsNotFound() {
            UserProfile userProfile = mock(UserProfile.class);
            when(getUserProfileOperation.execute(ArgumentMatchers.any())).thenReturn(userProfile);

            assertThrows(ResourceNotFoundException.class,
                () -> uiDefinitionController.getJurisdictions("create"));
        }

        @Test
        @DisplayName("should throw exception when empty jurisdictions found")
        void shouldThrownExceptionWhenJurisdictionsAreEmpty() {
            UserProfile userProfile = mock(UserProfile.class);
            JurisdictionDisplayProperties[] jurisdictionDisplayProperties = new JurisdictionDisplayProperties[0];
            when(userProfile.getJurisdictions()).thenReturn(jurisdictionDisplayProperties);
            when(getUserProfileOperation.execute(ArgumentMatchers.any())).thenReturn(userProfile);

            assertThrows(ResourceNotFoundException.class,
                () -> uiDefinitionController.getJurisdictions("create"));
        }

        @Test
        @DisplayName("should return jurisdiction resource when jurisdictions found")
        void shouldReturnJurisdictionResourceWhenJurisdictionsExists() {
            UserProfile userProfile = mock(UserProfile.class);
            JurisdictionDisplayProperties[] jurisdictionDisplayProperties = new JurisdictionDisplayProperties[2];
            JurisdictionDisplayProperties properties1 = mock(JurisdictionDisplayProperties.class);
            JurisdictionDisplayProperties properties2 = mock(JurisdictionDisplayProperties.class);
            jurisdictionDisplayProperties[0] = properties1;
            jurisdictionDisplayProperties[1] = properties2;
            when(userProfile.getJurisdictions()).thenReturn(jurisdictionDisplayProperties);
            when(getUserProfileOperation.execute(ArgumentMatchers.any())).thenReturn(userProfile);

            ResponseEntity<JurisdictionViewResource> response = uiDefinitionController.getJurisdictions("create");

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> assertEquals(2, response.getBody().getJurisdictions().length)
            );
        }
    }

    @Nested
    @DisplayName("GET /internal/jurisdiction-ui-configs")
    class GetJurisdictionUiConfigsDefinition {

        @Test
        @DisplayName("should return 200 when jurisdiction UI configs found")
        void caseFound() {
            final ResponseEntity<JurisdictionConfigViewResource> response = uiDefinitionController.getJurisdictionUiConfigs(Optional.of(jurisdictionReferenes));

            assertAll(
                () -> assertThat(response.getStatusCode(), is(HttpStatus.OK)),
                () -> {
                    JurisdictionConfigViewResource jurisdictionConfigViewResource = response.getBody();
                    assertThat(Lists.newArrayList(jurisdictionConfigViewResource.getConfigs()), hasItems(
                        hasProperty("id", is("Reference 1")),
                        hasProperty("id", is("Reference 2")),
                        hasProperty("name", is("Name 1")),
                        hasProperty("name", is("Name 2"))));
                }
            );
        }

        @Test
        @DisplayName("should propagate exception")
        void shouldPropagateExceptionWhenThrown() {
            when(getJurisdictionUiConfigOperation.execute(jurisdictionReferenes)).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class,
                () -> uiDefinitionController.getJurisdictionUiConfigs(Optional.of(jurisdictionReferenes)));
        }
    }
}
