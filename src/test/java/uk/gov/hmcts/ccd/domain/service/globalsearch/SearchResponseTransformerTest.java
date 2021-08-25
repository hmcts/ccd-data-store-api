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
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class SearchResponseTransformerTest extends TestFixtures {

    @Mock
    private DefaultCaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @InjectMocks
    private SearchResponseTransformer underTest;

    private static Map<String, JsonNode> CASE_DATA;
    private static ServiceLookup SERVICE_LOOKUP;
    private static LocationLookup LOCATION_LOOKUP;

    private final BuildingLocation location1 = BuildingLocation.builder()
        .buildingLocationId("L-1")
        .buildingLocationName("Location 1")
        .regionId("R-1")
        .region("Region 1")
        .build();
    private final BuildingLocation location2 = BuildingLocation.builder()
        .buildingLocationId("L-2")
        .buildingLocationName("Location 2")
        .regionId("R-2")
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

    @BeforeAll
    static void prepare() throws Exception {
        CASE_DATA = fromFileAsMap("case-data-with-case-reference.json");
        SERVICE_LOOKUP = new ServiceLookup(Map.of("SC1", "Service 1", "SC2", "Service 2"));
        LOCATION_LOOKUP = new LocationLookup(
            Map.of("L-1", "Location 1", "L-2", "Location 2"),
            Map.of("R-1", "Region 1", "R-2", "Region 2")
        );
    }

    @Test
    void testLocationLookupBuilderFunction() {
        // GIVEN
        final List<BuildingLocation> refData = List.of(location1, location2);

        // WHEN
        final LocationLookup locationLookup = SearchResponseTransformer.LOCATION_LOOKUP_FUNCTION.apply(refData);

        // THEN
        assertThat(locationLookup)
            .isNotNull()
            .satisfies(dictionary -> {
                assertThat(dictionary.getLocationName("L-1")).isEqualTo("Location 1");
                assertThat(dictionary.getLocationName("L-0")).isNull();
                assertThat(dictionary.getLocationName(null)).isNull();
                assertThat(dictionary.getRegionName("R-2")).isEqualTo("Region 2");
                assertThat(dictionary.getRegionName("R-0")).isNull();
                assertThat(dictionary.getRegionName(null)).isNull();
            });
    }

    @Test
    void testServiceLookupBuilderFunction() {
        // GIVEN
        final List<Service> refData = List.of(service1, service2);

        // WHEN
        final ServiceLookup serviceLookup = SearchResponseTransformer.SERVICE_LOOKUP_FUNCTION.apply(refData);

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
    void testShouldFindCaseReference() {
        final String actualResult = underTest.findValue(CASE_DATA, "CaseReference");

        assertThat(actualResult)
            .isNotNull()
            .isEqualTo("1629297445116784");
    }

    @Test
    void testShouldMapState() {
        // GIVEN
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withState("CaseCreated")
            .withVersion(VERSION_NUMBER)
            .withData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult = underTest.aResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

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
            .withVersion(VERSION_NUMBER)
            .withData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult = underTest.aResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

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
            .withCaseTypeId(CASE_TYPE_ID)
            .withVersion(VERSION_NUMBER)
            .withData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult = underTest.aResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

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
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(VERSION_NUMBER, CASE_TYPE_ID);
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withJurisdiction(JURISDICTION_ID)
            .withCaseTypeId(CASE_TYPE_ID)
            .withVersion(VERSION_NUMBER)
            .withData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult = underTest.aResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

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
            .withVersion(VERSION_NUMBER)
            .withData(CASE_DATA)
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult = underTest.aResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

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
            .withVersion(VERSION_NUMBER)
            .withData(CASE_DATA)
            .build();

        // WHEN
        final GlobalSearchResponse.Result actualResult = underTest.aResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getBaseLocationId()).isEqualTo("L-1");
                assertThat(result.getBaseLocationName()).isEqualTo("Location 1");
                assertThat(result.getRegionId()).isEqualTo("R-2");
                assertThat(result.getRegionName()).isEqualTo("Region 2");
            });
    }
}
