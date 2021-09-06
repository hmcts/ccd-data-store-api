package uk.gov.hmcts.ccd.domain.service.globalsearch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;

@Named
public class SearchResponseTransformer {

    private static final String SERVICE_ID_FIELD = "HMCTSServiceId";
    private static final String CASE_MANAGEMENT_LOCATION_FIELD = "caseManagementLocation";
    private static final String CASE_MANAGEMENT_CATEGORY_FIELD = "caseManagementCategory";
    private static final String CATEGORY_ID_PATH = "/value/code";
    private static final String CATEGORY_NAME_PATH = "/value/label";
    private static final String BASE_LOCATION_ID_PATH = "/baseLocation";
    private static final String REGION_ID_PATH = "/region";
    private static final String CASE_NAME_HMCTS_INTERNAL_FIELD = "caseNameHmctsInternal";
    private static final String SEARCH_CRITERIA_FIELD = "SearchCriteria";
    private static final String OTHER_CASE_REFERENCES_FIELD = "OtherCaseReferences";
    private static final String OTHER_CASE_REFERENCES_VALUE_FIELD = "value";

    private final CaseDefinitionRepository caseDefinitionRepository;

    @Inject
    public SearchResponseTransformer(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                         final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public GlobalSearchResponse.ResultInfo transformResultInfo(final Integer maxReturnRecordCount,
                                                               final Integer startRecordNumber,
                                                               final Long totalSearchHits,
                                                               final Integer recordsReturnedCount) {
        final boolean moreToGo = totalSearchHits > (maxReturnRecordCount + startRecordNumber - 1);

        return GlobalSearchResponse.ResultInfo.builder()
            .casesReturned(recordsReturnedCount)
            .caseStartRecord(startRecordNumber)
            .moreResultsToGo(moreToGo)
            .build();
    }

    public GlobalSearchResponse.Result transformResult(final CaseDetails caseDetails,
                                                       final ServiceLookup serviceLookup,
                                                       final LocationLookup locationLookup) {
        final String jurisdictionId = caseDetails.getJurisdiction();
        final String caseTypeId = caseDetails.getCaseTypeId();
        final Optional<JurisdictionDefinition> optionalJurisdiction =
            Optional.ofNullable(caseDefinitionRepository.getJurisdiction(jurisdictionId));
        final String caseTypeName = Optional.ofNullable(caseDefinitionRepository.getCaseType(caseTypeId))
            .map(CaseTypeDefinition::getName)
            .orElse(null);
        final Map<String, JsonNode> caseData = caseDetails.getData();

        final String serviceId = findValue(caseDetails.getSupplementaryData(), SERVICE_ID_FIELD);

        final String baseLocationId = findValue(caseData, CASE_MANAGEMENT_LOCATION_FIELD, BASE_LOCATION_ID_PATH);
        final String regionId = findValue(caseData, CASE_MANAGEMENT_LOCATION_FIELD, REGION_ID_PATH);

        return GlobalSearchResponse.Result.builder()
            .stateId(caseDetails.getState())
            .caseReference(caseDetails.getReferenceAsString())
            .otherReferences(getOtherReferences(caseData))
            .ccdJurisdictionId(jurisdictionId)
            .ccdJurisdictionName(optionalJurisdiction.map(JurisdictionDefinition::getName).orElse(null))
            .ccdCaseTypeId(caseTypeId)
            .ccdCaseTypeName(caseTypeName)
            .caseNameHmctsInternal(findValue(caseData, CASE_NAME_HMCTS_INTERNAL_FIELD))
            .hmctsServiceId(serviceId)
            .hmctsServiceShortDescription(serviceLookup.getServiceShortDescription(serviceId))
            .baseLocationId(baseLocationId)
            .baseLocationName(locationLookup.getLocationName(baseLocationId))
            .regionId(regionId)
            .regionName(locationLookup.getRegionName(regionId))
            .caseManagementCategoryId(findValue(caseData, CASE_MANAGEMENT_CATEGORY_FIELD, CATEGORY_ID_PATH))
            .caseManagementCategoryName(findValue(caseData, CASE_MANAGEMENT_CATEGORY_FIELD, CATEGORY_NAME_PATH))
            .build();
    }

    private String findValue(@NonNull final Map<String, JsonNode> jsonNodeMap, @NonNull final String key) {
        return findValue(jsonNodeMap, key, null);
    }

    private String findValue(@NonNull final Map<String, JsonNode> jsonNodeMap,
                             @NonNull final String parentKey,
                             final String childPath) {

        final Optional<JsonNode> optionalJsonNode = Optional.ofNullable(jsonNodeMap.get(parentKey));
        return optionalJsonNode.map(node -> {
            if (node.isContainerNode()) {
                return Optional.ofNullable(node.at(childPath)).map(JsonNode::asText).orElse(null);
            }
            return node.asText();
        }).orElse(null);
    }

    private List<String> getOtherReferences(@NonNull final Map<String, JsonNode> data) {
        final Optional<JsonNode> optionalJsonNode = Optional.ofNullable(data.get(SEARCH_CRITERIA_FIELD))
            .map(node -> node.get(OTHER_CASE_REFERENCES_FIELD));

        return optionalJsonNode.map(node -> StreamSupport.stream(node.spliterator(), false)
            .map(x -> x.get(OTHER_CASE_REFERENCES_VALUE_FIELD).asText())
            .collect(Collectors.toUnmodifiableList()))
            .orElse(emptyList());
    }

}
