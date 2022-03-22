package uk.gov.hmcts.ccd.domain.service.search.global;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.CaseAccessMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.domain.model.refdata.LocationLookup;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceLookup;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseManagementLocationFields;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SearchCriteriaFields;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.SupplementaryDataFields;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.domain.types.DynamicListValidator;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Collections.emptyList;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.CASE_MANAGEMENT_CATEGORY;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.CASE_MANAGEMENT_LOCATION;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.CASE_NAME_HMCTS_INTERNAL;
import static uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchFields.CaseDataFields.SEARCH_CRITERIA;

@SuppressWarnings("squid:S1075") // paths below are not URI path literals
@Named
public class GlobalSearchResponseTransformer {

    private static final String CATEGORY_VALUE_PATH = "/" + DynamicListValidator.VALUE;
    private static final String CATEGORY_ID_PATH = CATEGORY_VALUE_PATH + "/" + DynamicListValidator.CODE;
    private static final String CATEGORY_NAME_PATH = CATEGORY_VALUE_PATH + "/" + DynamicListValidator.LABEL;
    private static final String BASE_LOCATION_ID_PATH = "/" + CaseManagementLocationFields.BASE_LOCATION;
    private static final String REGION_ID_PATH = "/" + CaseManagementLocationFields.REGION;

    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDataAccessControl caseDataAccessControl;

    @Inject
    public GlobalSearchResponseTransformer(@Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                               final CaseDefinitionRepository caseDefinitionRepository,
                                           CaseDataAccessControl caseDataAccessControl) {

        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDataAccessControl = caseDataAccessControl;
    }

    public GlobalSearchResponsePayload.ResultInfo transformResultInfo(final Integer maxReturnRecordCount,
                                                                      final Integer startRecordNumber,
                                                                      final Long totalSearchHits,
                                                                      final Integer recordsReturnedCount) {
        final boolean moreToGo = totalSearchHits > (maxReturnRecordCount + startRecordNumber - 1);

        return GlobalSearchResponsePayload.ResultInfo.builder()
            .casesReturned(recordsReturnedCount)
            .caseStartRecord(startRecordNumber)
            .moreResultsToGo(moreToGo)
            .build();
    }

    public GlobalSearchResponsePayload.Result transformResult(final CaseDetails caseDetails,
                                                              final ServiceLookup serviceLookup,
                                                              final LocationLookup locationLookup) {
        final String jurisdictionId = caseDetails.getJurisdiction();
        final String caseTypeId = caseDetails.getCaseTypeId();

        final Optional<CaseTypeDefinition> optionalCaseType =
            Optional.ofNullable(caseDefinitionRepository.getCaseType(caseTypeId));
        Optional<JurisdictionDefinition> optionalJurisdiction =
            optionalCaseType.map(CaseTypeDefinition::getJurisdictionDefinition);
        // clear jurisdiction loaded from case type if ID doesn't match value from case details
        if (!StringUtils.equals(optionalJurisdiction.map(JurisdictionDefinition::getId).orElse(null), jurisdictionId)) {
            optionalJurisdiction = Optional.empty();
        }
        final String caseTypeName = optionalCaseType
            .map(CaseTypeDefinition::getName)
            .orElse(null);

        // NB: if no relevant case data has been index for record then use empty map rather than null
        final Map<String, JsonNode> caseData = Optional.ofNullable(caseDetails.getData()).orElse(new HashMap<>());

        final String serviceId = Optional.ofNullable(caseDetails.getSupplementaryData())
            .map(supplementaryData -> findValue(supplementaryData, SupplementaryDataFields.SERVICE_ID))
            .orElse(null);

        final String baseLocationId = findValue(caseData, CASE_MANAGEMENT_LOCATION, BASE_LOCATION_ID_PATH);
        final String regionId = findValue(caseData, CASE_MANAGEMENT_LOCATION, REGION_ID_PATH);

        return GlobalSearchResponsePayload.Result.builder()
            .stateId(caseDetails.getState())
            .caseReference(caseDetails.getReferenceAsString())
            .otherReferences(getOtherReferences(caseData))
            .ccdJurisdictionId(jurisdictionId)
            .ccdJurisdictionName(optionalJurisdiction.map(JurisdictionDefinition::getName).orElse(null))
            .ccdCaseTypeId(caseTypeId)
            .ccdCaseTypeName(caseTypeName)
            .caseNameHmctsInternal(findValue(caseData, CASE_NAME_HMCTS_INTERNAL))
            .hmctsServiceId(serviceId)
            .hmctsServiceShortDescription(serviceLookup.getServiceShortDescription(serviceId))
            .baseLocationId(baseLocationId)
            .baseLocationName(locationLookup.getLocationName(baseLocationId))
            .regionId(regionId)
            .regionName(locationLookup.getRegionName(regionId))
            .caseManagementCategoryId(findValue(caseData, CASE_MANAGEMENT_CATEGORY, CATEGORY_ID_PATH))
            .caseManagementCategoryName(findValue(caseData, CASE_MANAGEMENT_CATEGORY, CATEGORY_NAME_PATH))
            .processForAccess(getCaseAccessMetaData(caseDetails.getReferenceAsString()).getAccessProcessString())
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
        final Optional<JsonNode> optionalJsonNode = Optional.ofNullable(data.get(SEARCH_CRITERIA))
            .map(node -> node.get(SearchCriteriaFields.OTHER_CASE_REFERENCES));

        // get values from collection list
        return optionalJsonNode.map(node -> StreamSupport.stream(node.spliterator(), false)
            .map(x -> x.get(CollectionValidator.VALUE).asText())
            .collect(Collectors.toUnmodifiableList()))
            .orElse(emptyList());
    }

    private CaseAccessMetadata getCaseAccessMetaData(String caseReference) {
        return caseDataAccessControl.generateAccessMetadata(caseReference);
    }

}
