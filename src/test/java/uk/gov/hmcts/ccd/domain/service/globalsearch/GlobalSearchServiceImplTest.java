package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.service.aggregated.CaseDetailsUtil;

import java.util.List;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceImplTest extends TestFixtures {
    @Mock
    private ReferenceDataRepository referenceDataRepository;

    @Mock
    private SearchResponseTransformer searchResponseTransformer;

    @InjectMocks
    private GlobalSearchServiceImpl underTest;

    @Test
    void testLocationLookupBuilderFunction() {
        // WHEN
        final LocationLookup locationLookup =
            GlobalSearchServiceImpl.LOCATION_LOOKUP_FUNCTION.apply(locationsRefData);

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
        final ServiceLookup serviceLookup = GlobalSearchServiceImpl.SERVICE_LOOKUP_FUNCTION.apply(servicesRefData);

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
    void testShouldTransformSearchResult() {
        // GIVEN
        final GlobalSearchRequestPayload requestPayload = new GlobalSearchRequestPayload();
        final CaseSearchResult caseSearchResult = buildCaseSearchResult();
        doReturn(servicesRefData).when(referenceDataRepository).getServices();
        doReturn(locationsRefData).when(referenceDataRepository).getBuildingLocations();
        doReturn(GlobalSearchResponse.ResultInfo.builder().build()).when(searchResponseTransformer).transformResultInfo(
            requestPayload.getMaxReturnRecordCount(),
            requestPayload.getStartRecordNumber(),
            caseSearchResult.getTotal(),
            caseSearchResult.getCases().size()
        );
        doReturn(GlobalSearchResponse.Result.builder().build()).when(searchResponseTransformer).transformResult(
            any(CaseDetails.class),
            any(ServiceLookup.class),
            any(LocationLookup.class)
        );

        // WHEN
        final GlobalSearchResponse response = underTest.transformResponse(requestPayload, caseSearchResult);

        // THEN
        assertThat(response).isNotNull();

        verify(searchResponseTransformer).transformResultInfo(
            requestPayload.getMaxReturnRecordCount(),
            requestPayload.getStartRecordNumber(),
            caseSearchResult.getTotal(),
            caseSearchResult.getCases().size()
        );
        verify(searchResponseTransformer).transformResult(
            any(CaseDetails.class),
            any(ServiceLookup.class),
            any(LocationLookup.class)
        );
    }

    private CaseSearchResult buildCaseSearchResult() {
        final CaseDetails caseDetails = CaseDetailsUtil.CaseDetailsBuilder.caseDetails()
            .withReference(1629297445116784L)
            .withState("CaseCreated")
            .withJurisdiction(JURISDICTION_ID)
            .withCaseTypeId(CASE_TYPE_ID)
            .withData(emptyMap())
            .withSupplementaryData(emptyMap())
            .build();
        return new CaseSearchResult(1L, List.of(caseDetails));
    }

}
