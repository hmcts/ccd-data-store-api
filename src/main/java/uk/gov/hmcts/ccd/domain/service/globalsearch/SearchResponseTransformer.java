package uk.gov.hmcts.ccd.domain.service.globalsearch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.DefaultCaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.LocationCollector.toLocationLookup;

@Named
public class SearchResponseTransformer {

    private static final String SERVICE_ID_FIELD = "service_code";
    private static final String BASE_LOCATION_ID_FIELD = "base_location_id";
    private static final String REGION_ID_FIELD = "region_id";

    static Function<List<BuildingLocation>, LocationLookup> LOCATION_LOOKUP_FUNCTION =
        locations -> locations.stream().collect(toLocationLookup());

    static Function<List<Service>, ServiceLookup> SERVICE_LOOKUP_FUNCTION = services -> {
        final Map<String, String> servicesMap = services.stream()
            .collect(toUnmodifiableMap(Service::getServiceCode, Service::getServiceShortDescription));
        return new ServiceLookup(servicesMap);
    };

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final ReferenceDataRepository referenceDataRepository;

    @Inject
    public SearchResponseTransformer(@Qualifier(DefaultCaseDefinitionRepository.QUALIFIER)
                                         final CaseDefinitionRepository caseDefinitionRepository,
                                     final ReferenceDataRepository referenceDataRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.referenceDataRepository = referenceDataRepository;
    }

    public GlobalSearchResponse transform(final CaseSearchResult caseSearchResult) {

        final List<Service> services = referenceDataRepository.getServices();
        final List<BuildingLocation> buildingLocations = referenceDataRepository.getBuildingLocations();
        final ServiceLookup serviceLookup = SERVICE_LOOKUP_FUNCTION.apply(services);
        final LocationLookup locationLookup = LOCATION_LOOKUP_FUNCTION.apply(buildingLocations);

        return null;
    }

    GlobalSearchResponse.Result transformResult(final CaseDetails caseDetails,
                                                final ServiceLookup serviceLookup,
                                                final LocationLookup locationLookup) {
        final String jurisdictionId = caseDetails.getJurisdiction();
        final String caseTypeId = caseDetails.getCaseTypeId();
        final Optional<JurisdictionDefinition> optionalJurisdiction =
            Optional.ofNullable(caseDefinitionRepository.getJurisdiction(jurisdictionId));
        final Optional<CaseTypeDefinition> optionalCaseType =
            findCaseTypeDefinition(caseTypeId, caseDetails.getVersion());
        final Map<String, JsonNode> caseData = caseDetails.getData();
        final String serviceId = findValue(caseData, SERVICE_ID_FIELD);
        final String baseLocationId = findValue(caseData, BASE_LOCATION_ID_FIELD);
        final String regionId = findValue(caseData, REGION_ID_FIELD);

        return GlobalSearchResponse.Result.builder()
            .stateId(caseDetails.getState())
            .caseReference(caseDetails.getReferenceAsString())
            .ccdJurisdictionId(jurisdictionId)
            .ccdJurisdictionName(optionalJurisdiction.map(JurisdictionDefinition::getName).orElse(null))
            .ccdCaseTypeId(caseTypeId)
            .ccdCaseTypeName(optionalCaseType.map(CaseTypeDefinition::getName).orElse(null))
            .hmctsServiceId(serviceId)
            .hmctsServiceShortDescription(serviceLookup.getServiceShortDescription(serviceId))
            .baseLocationId(baseLocationId)
            .baseLocationName(locationLookup.getLocationName(baseLocationId))
            .regionId(regionId)
            .regionName(locationLookup.getRegionName(regionId))
            .build();
    }

    String findValue(@NonNull final Map<String, JsonNode> data, @NonNull final String fieldName) {
        final Optional<JsonNode> optionalJsonNode = findNode(data, fieldName);
        return optionalJsonNode
            .map(node -> node.get(fieldName).asText())
            .orElse(null);
    }

    private Optional<JsonNode> findNode(@NonNull final Map<String, JsonNode> data, @NonNull final String fieldName) {
        return data.values().stream()
            .map(node -> node.findParents(fieldName))
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .findFirst();
    }

    private Optional<CaseTypeDefinition> findCaseTypeDefinition(final String caseTypeId, final Integer version) {
        return Optional.ofNullable(version)
            .map(v -> caseDefinitionRepository.getCaseType(v, caseTypeId))
            .or(() -> Optional.ofNullable(caseDefinitionRepository.getCaseType(caseTypeId)));
    }

}
