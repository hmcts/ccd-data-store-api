package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.LocationCollector.toLocationLookup;

@Service
public class GlobalSearchServiceImpl implements GlobalSearchService {
    private final ReferenceDataRepository referenceDataRepository;
    private final SearchResponseTransformer searchResponseTransformer;

    static Function<List<BuildingLocation>, LocationLookup> LOCATION_LOOKUP_FUNCTION =
        locations -> locations.stream().collect(toLocationLookup());

    static Function<List<ServiceReferenceData>, ServiceLookup> SERVICE_LOOKUP_FUNCTION =
        services -> {
            final Map<String, String> servicesMap = services.stream()
                .collect(toUnmodifiableMap(ServiceReferenceData::getServiceCode,
                    ServiceReferenceData::getServiceShortDescription));
            return new ServiceLookup(servicesMap);
        };

    @Autowired
    public GlobalSearchServiceImpl(final ReferenceDataRepository referenceDataRepository,
                                   final SearchResponseTransformer searchResponseTransformer) {
        this.referenceDataRepository = referenceDataRepository;
        this.searchResponseTransformer = searchResponseTransformer;
    }

    @Override
    public void assembleSearchQuery(GlobalSearchRequestPayload payload) {
    }

    public GlobalSearchResponse transformResponse(final GlobalSearchRequestPayload requestPayload,
                                                  final CaseSearchResult caseSearchResult) {
        final List<ServiceReferenceData> services = referenceDataRepository.getServices();
        final List<BuildingLocation> buildingLocations = referenceDataRepository.getBuildingLocations();
        final ServiceLookup serviceLookup = SERVICE_LOOKUP_FUNCTION.apply(services);
        final LocationLookup locationLookup = LOCATION_LOOKUP_FUNCTION.apply(buildingLocations);

        final List<GlobalSearchResponse.Result> results = caseSearchResult.getCases().stream()
            .map(caseDetails -> searchResponseTransformer.transformResult(caseDetails, serviceLookup, locationLookup))
            .collect(Collectors.toUnmodifiableList());

        final GlobalSearchResponse.ResultInfo resultInfo = searchResponseTransformer.transformResultInfo(
            requestPayload.getMaxReturnRecordCount(),
            requestPayload.getStartRecordNumber(),
            caseSearchResult.getTotal(),
            results.size()
        );

        return GlobalSearchResponse.builder()
            .resultInfo(resultInfo)
            .results(results)
            .build();
    }
}
