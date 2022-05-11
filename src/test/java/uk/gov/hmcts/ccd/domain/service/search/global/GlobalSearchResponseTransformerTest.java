package uk.gov.hmcts.ccd.domain.service.search.global;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProcess;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class GlobalSearchResponseTransformerTest extends TestFixtures {

    protected static final String JURISDICTION_ID_DIFFERENT = "DIFFERENT";

    @Mock
    private CachedCaseDefinitionRepository caseDefinitionRepository;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @InjectMocks
    private GlobalSearchResponseTransformer underTest;

    private static Map<String, JsonNode> CASE_DATA;
    private static Map<String, JsonNode> SUPPLEMENTARY_DATA;
    private static ServiceLookup SERVICE_LOOKUP;
    private static LocationLookup LOCATION_LOOKUP;

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
    void testShouldMapState() {
        // GIVEN
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withState("CaseCreated")
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getStateId()).isEqualTo("CaseCreated"));
    }

    @Test
    void testShouldMapCaseReference() {
        // GIVEN
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withReference(1629297445116784L)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCaseReference()).isEqualTo("1629297445116784"));
    }

    @Test
    void testShouldMapJurisdictionWithName() {
        // GIVEN
        stubAccessMetadata();
        final JurisdictionDefinition jurisdictionDefinition = buildJurisdictionDefinition();
        final CaseTypeDefinition caseTypeDefinition = buildCaseTypeDefinition();
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withJurisdiction(JURISDICTION_ID)
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
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
    void testShouldMapJurisdictionWithoutNameWhenIDsDoNotMatch() {
        // GIVEN
        stubAccessMetadata();
        final JurisdictionDefinition jurisdictionDefinition = buildJurisdictionDefinition();
        final CaseTypeDefinition caseTypeDefinition = buildCaseTypeDefinition();
        caseTypeDefinition.setJurisdictionDefinition(jurisdictionDefinition);
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withJurisdiction(JURISDICTION_ID_DIFFERENT)
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> {
                assertThat(result.getCcdJurisdictionId()).isEqualTo(JURISDICTION_ID_DIFFERENT);
                assertThat(result.getCcdJurisdictionName()).isNull();  // i.e. not mapped
            });
    }

    @Test
    void testShouldMapCaseType() {
        // GIVEN
        stubAccessMetadata();
        final CaseTypeDefinition caseTypeDefinition = buildCaseTypeDefinition();
        doReturn(caseTypeDefinition).when(caseDefinitionRepository).getCaseType(CASE_TYPE_ID);
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
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
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withSupplementaryData(SUPPLEMENTARY_DATA)
            .withData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
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
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
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
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
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
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCaseNameHmctsInternal()).isEqualTo("Internal case name"));
    }

    private void stubAccessMetadata() {
        val caseAccessMetadata = new CaseAccessMetadata();
        caseAccessMetadata.setAccessProcess(AccessProcess.CHALLENGED);
        doReturn(caseAccessMetadata).when(caseDataAccessControl).generateAccessMetadata(any());
    }

    @Test
    void testShouldMapOtherCaseReferences() {
        // GIVEN
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getOtherReferences()).containsExactlyInAnyOrder("Ref1", "Ref2"));
    }

    @Test
    void testShouldHandleNullCaseDataGracefully() {
        // GIVEN
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(null) // i.e. when no GlobalSearch fields present in case data
            .withReference(REFERENCE)
            .withSupplementaryData(emptyMap())
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCaseReference()).isEqualTo(REFERENCE.toString()));
    }

    @Test
    void testShouldHandleNullSupplementaryDataGracefully() {
        // GIVEN
        stubAccessMetadata();
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withData(CASE_DATA)
            .withReference(REFERENCE)
            .withSupplementaryData(emptyMap()) // i.e. when no GlobalSearch fields present in SupplementaryData
            .build();

        // WHEN
        final GlobalSearchResponsePayload.Result actualResult =
            underTest.transformResult(caseDetails, SERVICE_LOOKUP, LOCATION_LOOKUP);

        // THEN
        assertThat(actualResult)
            .isNotNull()
            .satisfies(result -> assertThat(result.getCaseReference()).isEqualTo(REFERENCE.toString()));
    }

    @ParameterizedTest
    @MethodSource("providePaginationParameters")
    void testShouldEvaluatePaginationInformation(final Integer maxReturnRecordCount,
                                                 final Integer startRecordNumber,
                                                 final Long totalSearchHits,
                                                 final Integer recordsReturnedCount,
                                                 final Boolean moreToGo) {
        final GlobalSearchResponsePayload.ResultInfo actualResultInfo = underTest.transformResultInfo(
            maxReturnRecordCount,
            startRecordNumber,
            totalSearchHits,
            recordsReturnedCount
        );

        assertThat(actualResultInfo)
            .isNotNull()
            .satisfies(resultInfo -> {
                assertThat(resultInfo.getCasesReturned()).isEqualTo(recordsReturnedCount);
                assertThat(resultInfo.getCaseStartRecord()).isEqualTo(startRecordNumber);
                assertThat(resultInfo.isMoreResultsToGo()).isEqualTo(moreToGo);
            });
    }

    private static Stream<Arguments> providePaginationParameters() {
        return Stream.of(
            Arguments.of(10, 1, 11L, 10, true),
            Arguments.of(5, 5, 10L, 5, true),
            Arguments.of(10, 1, 10L, 10, false),
            Arguments.of(5, 6, 10L, 5, false),
            Arguments.of(5, 1, 2L, 2, false)
        );
    }

}
