package uk.gov.hmcts.ccd.domain.service.globalsearch;

import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.LocationCollector.toLocationLookup;

@Named
public class SearchResponseTransformer {

    static Function<List<BuildingLocation>, LocationLookup> LOCATION_LOOKUP_FUNCTION =
        locations -> locations.stream().collect(toLocationLookup());

    static Function<List<Service>, ServiceLookup> SERVICE_LOOKUP_FUNCTION = services -> {
        final Map<String, String> servicesMap = services.stream()
            .collect(toUnmodifiableMap(Service::getServiceCode, Service::getServiceShortDescription));
        return new ServiceLookup(servicesMap);
    };

    private final ReferenceDataRepository referenceDataRepository;

    @Inject
    public SearchResponseTransformer(ReferenceDataRepository referenceDataRepository) {
        this.referenceDataRepository = referenceDataRepository;
    }

    public GlobalSearchResponse transform(final CaseSearchResult caseSearchResult) {
        return null;
    }

}
