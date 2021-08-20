package uk.gov.hmcts.ccd.domain.service.globalsearch;

import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.search.global.ServiceLookup;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.LocationCollector.toLocationLookup;

public class SearchResponseTransformer {

    static Function<List<LocationRefData>, LocationLookup> LOCATION_LOOKUP_FUNCTION =
        locations -> locations.stream().collect(toLocationLookup());

    static Function<List<ServiceRefData>, ServiceLookup> SERVICE_LOOKUP_FUNCTION = services -> {
        final Map<String, String> servicesMap = services.stream()
            .collect(toUnmodifiableMap(ServiceRefData::getServiceCode, ServiceRefData::getServiceShortDescription));
        return new ServiceLookup(servicesMap);
    };

    public GlobalSearchResponse transform(final CaseSearchResult caseSearchResult) {
        return null;
    }

}
