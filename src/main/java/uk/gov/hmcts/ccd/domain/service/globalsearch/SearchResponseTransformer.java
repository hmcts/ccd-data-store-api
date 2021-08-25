package uk.gov.hmcts.ccd.domain.service.globalsearch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.Service;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toUnmodifiableMap;
import static uk.gov.hmcts.ccd.domain.service.globalsearch.LocationCollector.toLocationLookup;

@Named
public class SearchResponseTransformer {

    private static final String SERVICE_ID_FIELD = "HMCTSServiceId";
    private static final String CASE_MANAGEMENT_LOCATION_FIELD = "caseManagementLocation";
    private static final String CASE_MANAGEMENT_CATEGORY_FIELD = "caseManagementCategory";
    private static final String VALUE_FIELD = "value";
    private static final String CASE_MANAGEMENT_CATEGORY_ID_FIELD = "code";
    private static final String CASE_MANAGEMENT_CATEGORY_NAME_FIELD = "label";
    private static final String BASE_LOCATION_ID_FIELD = "baseLocation";
    private static final String REGION_ID_FIELD = "region";
    private static final String CASE_NAME_HMCTS_INTERNAL_FIELD = "caseNameHmctsInternal";
    private static final String SEARCH_CRITERIA_FIELD = "SearchCriteria";
    private static final String OTHER_CASE_REFERENCES_FIELD = "OtherCaseReferences";
    private static final String ACCESS_PROCESS = "access_process";

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
    public SearchResponseTransformer(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
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

        final String serviceId = getServiceId(caseDetails.getSupplementaryData());

        final Tuple2<String, String> caseManagementLocation = getCaseManagementLocation(caseData);
        final String baseLocationId = caseManagementLocation.v1;
        final String regionId = caseManagementLocation.v2;

        final Tuple2<String, String> caseManagementCategory = getCaseManagementCategory(caseData);
        final String categoryId = caseManagementCategory.v1;
        final String categoryName = caseManagementCategory.v2;

        return GlobalSearchResponse.Result.builder()
            .stateId(caseDetails.getState())
            .caseReference(caseDetails.getReferenceAsString())
            .otherReferences(getOtherReferences(caseData))
            .ccdJurisdictionId(jurisdictionId)
            .ccdJurisdictionName(optionalJurisdiction.map(JurisdictionDefinition::getName).orElse(null))
            .ccdCaseTypeId(caseTypeId)
            .ccdCaseTypeName(optionalCaseType.map(CaseTypeDefinition::getName).orElse(null))
            .caseNameHmctsInternal(findStringValue(caseData, CASE_NAME_HMCTS_INTERNAL_FIELD))
            .hmctsServiceId(serviceId)
            .hmctsServiceShortDescription(serviceLookup.getServiceShortDescription(serviceId))
            .baseLocationId(baseLocationId)
            .baseLocationName(locationLookup.getLocationName(baseLocationId))
            .regionId(regionId)
            .regionName(locationLookup.getRegionName(regionId))
            .caseManagementCategoryId(categoryId)
            .caseManagementCategoryName(categoryName)
            .build();
    }

    private String findStringValue(@NonNull final Map<String, JsonNode> data,
                                               @NonNull final String fieldName) {
        return Optional.ofNullable(data.get(fieldName)).map(JsonNode::asText).orElse(null);
    }

    private Optional<JsonNode> findNode(@NonNull final Map<String, JsonNode> data, @NonNull final String fieldName) {
        return Optional.ofNullable(data.get(fieldName));
    }

    private Tuple2<String, String> getCaseManagementLocation(@NonNull final Map<String, JsonNode> data) {
        final Optional<JsonNode> optionalJsonNode = findNode(data, CASE_MANAGEMENT_LOCATION_FIELD);
        return optionalJsonNode
            .map(node -> new Tuple2<>(node.get(BASE_LOCATION_ID_FIELD).asText(), node.get(REGION_ID_FIELD).asText()))
            .orElse(new Tuple2<>(null, null));
    }

    private Tuple2<String, String> getCaseManagementCategory(@NonNull final Map<String, JsonNode> data) {
        final Optional<JsonNode> optionalJsonNode = findNode(data, CASE_MANAGEMENT_CATEGORY_FIELD)
            .map(node -> node.get(VALUE_FIELD));

        return optionalJsonNode
            .map(node -> new Tuple2<>(node.get(CASE_MANAGEMENT_CATEGORY_ID_FIELD).asText(),
                node.get(CASE_MANAGEMENT_CATEGORY_NAME_FIELD).asText()))
            .orElse(new Tuple2<>(null, null));
    }

    private String getServiceId(@NonNull final Map<String, JsonNode> supplementaryData) {
        return findStringValue(supplementaryData, SERVICE_ID_FIELD);
    }

    private List<String> getOtherReferences(@NonNull final Map<String, JsonNode> data) {
        final Optional<JsonNode> optionalJsonNode = findNode(data, SEARCH_CRITERIA_FIELD)
            .map(node -> node.get(OTHER_CASE_REFERENCES_FIELD));

        return optionalJsonNode.map(node -> StreamSupport.stream(node.spliterator(), false)
            .map(x -> x.get(VALUE_FIELD).asText())
            .collect(Collectors.toUnmodifiableList()))
            .orElse(Collections.emptyList());
    }

    private Optional<CaseTypeDefinition> findCaseTypeDefinition(final String caseTypeId, final Integer version) {
        return Optional.ofNullable(version)
            .map(v -> caseDefinitionRepository.getCaseType(v, caseTypeId))
            .or(() -> Optional.ofNullable(caseDefinitionRepository.getCaseType(caseTypeId)));
    }

}
