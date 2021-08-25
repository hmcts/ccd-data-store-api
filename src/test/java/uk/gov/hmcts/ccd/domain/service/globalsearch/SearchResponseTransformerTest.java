package uk.gov.hmcts.ccd.domain.service.globalsearch;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SearchResponseTransformerTest extends TestFixtures {

    @Mock
    private CachedCaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @InjectMocks
    private SearchResponseTransformer underTest;

    private static Map<String, JsonNode> CASE_DATA;
    private static Map<String, JsonNode> SUPPLEMENTARY_DATA;
    private static ServiceLookup SERVICE_LOOKUP;
    private static LocationLookup LOCATION_LOOKUP;

    private final BuildingLocation location1 = BuildingLocation.builder()
        .buildingLocationId("321")
        .buildingLocationName("Location 1")
        .regionId("R-1")
        .region("Region 1")
        .build();
    private final BuildingLocation location2 = BuildingLocation.builder()
        .buildingLocationId("L-2")
        .buildingLocationName("Location 2")
        .regionId("123")
        .region("Region 2")
        .build();

    private final Service service1 = Service.builder()
        .serviceCode("SC1")
        .serviceShortDescription("Service 1")
        .build();
    private final Service service2 = Service.builder()
        .serviceCode("SC2")
        .serviceShortDescription("Service 2")
        .build();

    private final List<BuildingLocation> locationsRefData = List.of(location1, location2);
    private final List<Service> servicesRefData = List.of(service1, service2);

    private final String categoryId = "CATEGORY-A";
    private final String categoryName = "This is the description for category A.";

    @BeforeAll
    static void prepare() throws Exception {
        CASE_DATA = fromFileAsMap("global-search-result-data.json");
        SUPPLEMENTARY_DATA = fromFileAsMap("global-search-result-supplementary-data.json");
        SERVICE_LOOKUP = new ServiceLookup(Map.of("SC1", "Service 1", "SC2", "Service 2"));
        LOCATION_LOOKUP = new LocationLookup(
            Map.of("321", "Location 1", "L-2", "Location 2"),
            Map.of("R-1", "Region 1", "123", "Region 2")
        );
    }

    @Test
    void testLocationLookupBuilderFunction() {
        // WHEN
        final LocationLookup locationLookup =
            SearchResponseTransformer.LOCATION_LOOKUP_FUNCTION.apply(locationsRefData);

        // THEN
        assertThat(locationLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getLocationName("321")).isEqualTo("Location 1");
                assertThat(dictionary.getLocationName("L-0")).isNull();
                assertThat(dictionary.getLocationName(null)).isNull();
                assertThat(dictionary.getRegionName("123")).isEqualTo("Region 2");
                assertThat(dictionary.getRegionName("R-0")).isNull();
                assertThat(dictionary.getRegionName(null)).isNull();
            });
    }

    @Test
    void testServiceLookupBuilderFunction() {
        // WHEN
        final ServiceLookup serviceLookup = SearchResponseTransformer.SERVICE_LOOKUP_FUNCTION.apply(servicesRefData);

        // THEN
        assertThat(serviceLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getServiceShortDescription("SC1")).isEqualTo("Service 1");
                assertThat(dictionary.getServiceShortDescription("SC2")).isEqualTo("Service 2");
                assertThat(dictionary.getServiceShortDescription("SC3")).isNull();
                assertThat(dictionary.getServiceShortDescription(null)).isNull();
            });
    }

    @Test
    void testShouldMapState() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withState("CaseCreated")
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getStateId()).isEqualTo("CaseCreated"));
    }

    @Test
    void testShouldMapCaseReference() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withReference(1629297445116784L)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCaseReference()).isEqualTo("1629297445116784"));
    }

    @Test
    void testShouldMapJurisdiction() {
        // GIVEN
        final JurisdictionDefinition jurisdictionDefinition = buildJurisdictionDefinition();
        doReturn(jurisdictionDefinition).when(caseDefinitionRepository).getJurisdiction(JURISDICTION_ID);
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withJurisdiction(JURISDICTION_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getCcdJurisdictionId()).isEqualTo(JURISDICTION_ID);
                assertThat(result.getCcdJurisdictionName()).isEqualTo(JURISDICTION_NAME);
            });
    }

    @Test
    void testShouldMapCaseType() {
        // GIVEN
        final CaseTypeDefinition caseTypeDefinition = buildCaseTypeDefinition();
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getCcdCaseTypeId()).isEqualTo(CASE_TYPE_ID);
                assertThat(result.getCcdCaseTypeName()).isEqualTo(CASE_TYPE_NAME);
            });
    }

    @Test
    void testShouldMapService() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withSupplementaryData(SUPPLEMENTARY_DATA)
            .withData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getHmctsServiceId()).isEqualTo("SC1");
                assertThat(result.getHmctsServiceShortDescription()).isEqualTo("Service 1");
            });
    }

    @Test
    void testShouldMapLocationAndRegion() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getBaseLocationId()).isEqualTo("321");
                assertThat(result.getBaseLocationName()).isEqualTo("Location 1");
                assertThat(result.getRegionId()).isEqualTo("123");
                assertThat(result.getRegionName()).isEqualTo("Region 2");
            });
    }

    @Test
    void testShouldMapCaseManagementCategory() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getCaseManagementCategoryId()).isEqualTo(categoryId);
                assertThat(result.getCaseManagementCategoryName()).isEqualTo(categoryName);
            });
    }

    @Test
    void testShouldMapCaseNameHmctsInternal() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCaseNameHmctsInternal()).isEqualTo("Internal case name"));
    }

    @Test
    void testShouldMapOtherCaseReferences() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getOtherReferences()).containsExactlyInAnyOrder("Ref1", "Ref2"));
    }

    @Test
    void testShouldOrchestrateResultTransformation() {
        // GIVEN
        final JurisdictionDefinition jurisdictionDefinition = buildJurisdictionDefinition();
        final CaseTypeDefinition caseTypeDefinition = buildCaseTypeDefinition();
        doReturn(jurisdictionDefinition).when(caseDefinitionRepository).getJurisdiction(JURISDICTION_ID);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        doReturn(servicesRefData).when(referenceDataRepository).getServices();
        doReturn(locationsRefData).when(referenceDataRepository).getBuildingLocations();

        final CaseSearchResult caseSearchResult = buildCaseSearchResult();

        // WHEN
        final GlobalSearchResponse actualResponse = underTest.transform(caseSearchResult);

        assertThat(actualResponse)
            .isNotNull()
            .satisfies(response -> {
                final List<GlobalSearchResponse.Result> results = response.getResults();
                assertThat(results).isNotEmpty();
                final GlobalSearchResponse.Result result = results.stream().findFirst().get();
                assertThat(result.getStateId()).isEqualTo("CaseCreated");
                assertThat(result.getCaseReference()).isEqualTo("1629297445116784");
                assertThat(result.getCcdJurisdictionId()).isEqualTo(JURISDICTION_ID);
                assertThat(result.getCcdJurisdictionName()).isEqualTo(JURISDICTION_NAME);
                assertThat(result.getCcdCaseTypeId()).isEqualTo(CASE_TYPE_ID);
                assertThat(result.getCcdCaseTypeName()).isEqualTo(CASE_TYPE_NAME);
                assertThat(result.getHmctsServiceId()).isEqualTo("SC1");
                assertThat(result.getHmctsServiceShortDescription()).isEqualTo("Service 1");
                assertThat(result.getBaseLocationId()).isEqualTo("321");
                assertThat(result.getBaseLocationName()).isEqualTo("Location 1");
                assertThat(result.getRegionId()).isEqualTo("123");
                assertThat(result.getRegionName()).isEqualTo("Region 2");
                assertThat(result.getCaseManagementCategoryId()).isEqualTo(categoryId);
                assertThat(result.getCaseManagementCategoryName()).isEqualTo(categoryName);
                assertThat(result.getCaseNameHmctsInternal()).isEqualTo("Internal case name");
                assertThat(result.getOtherReferences()).containsExactlyInAnyOrder("Ref1", "Ref2");
            });

        verify(caseDefinitionRepository).getJurisdiction(JURISDICTION_ID);
        verify(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        verify(referenceDataRepository).getServices();
        verify(referenceDataRepository).getBuildingLocations();
    }

    private CaseSearchResult buildCaseSearchResult() {
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withReference(1629297445116784L)
            .withState("CaseCreated")
            .withJurisdiction(JURISDICTION_ID)
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(CASE_DATA)
            .withSupplementaryData(SUPPLEMENTARY_DATA)
            .build();
        return new CaseSearchResult(1L, List.of(caseDetails));
    }

}
