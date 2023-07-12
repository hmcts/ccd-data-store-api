package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.LABEL;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
@SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
public class CaseTypeDefinition implements Serializable {
    private static final long serialVersionUID = 5688786015302840008L;
    String id;
    String description;
    Version version;
    String name;
    @JsonProperty("jurisdiction")
    JurisdictionDefinition jurisdictionDefinition;
    @JsonProperty("security_classification")
    SecurityClassification securityClassification;
    @Builder.Default
    private List<CaseEventDefinition> events = new ArrayList<>();
    @Builder.Default
    private List<CaseStateDefinition> states = new ArrayList<>();
    @JsonProperty("case_fields")
    @Builder.Default
    private List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
    @JsonProperty("printable_document_url")
    String printableDocumentsUrl;
    @JsonProperty("acls")
    @Builder.Default
    private List<AccessControlList> accessControlLists = new ArrayList<>();
    @JsonProperty("callback_get_case_url")
    String callbackGetCaseUrl;
    @JsonProperty("retries_get_case_url")
    private List<Integer> retriesGetCaseUrl;
    @Builder.Default
    private List<SearchAliasField> searchAliasFields = new ArrayList<>();
    @Builder.Default
    private List<SearchParty> searchParties = new ArrayList<>();
    @Builder.Default
    private List<SearchCriteria> searchCriterias = new ArrayList<>();
    @Builder.Default
    private List<CategoryDefinition> categories = new ArrayList<>();
    @JsonProperty("roleToAccessProfiles")
    @Builder.Default
    private List<RoleToAccessProfileDefinition> roleToAccessProfiles = new ArrayList<>();

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Version getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    @JsonIgnore
    public String getJurisdictionId() {
        return jurisdictionDefinition.getId();
    }

    public JurisdictionDefinition getJurisdictionDefinition() {
        return jurisdictionDefinition;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public List<CaseEventDefinition> getEvents() {
        return events;
    }

    public List<CaseStateDefinition> getStates() {
        return states;
    }

    public List<CaseFieldDefinition> getCaseFieldDefinitions() {
        return caseFieldDefinitions;
    }

    public String getPrintableDocumentsUrl() {
        return printableDocumentsUrl;
    }

    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public SecurityClassification getClassificationForField(String fieldId) {
        return SecurityClassification.valueOf(caseFieldDefinitions
            .stream()
            .filter(cf -> cf.getId().equals(fieldId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                String.format("CaseFieldId %s not found in CaseType %s", fieldId, name)))
            .getSecurityLabel());
    }

    public boolean hasDraftEnabledEvent() {
        return this.events
            .stream()
            .anyMatch(caseEvent -> caseEvent.getCanSaveDraft() != null && caseEvent.getCanSaveDraft());
    }

    public boolean hasEventId(String eventId) {
        return events.stream().anyMatch(event -> event.getId().equals(eventId));
    }

    public Optional<CaseEventDefinition> findCaseEvent(String eventId) {
        return events.stream()
            .filter(event -> event.getId().equalsIgnoreCase(eventId))
            .findFirst();
    }

    public String getCallbackGetCaseUrl() {
        return callbackGetCaseUrl;
    }

    public List<Integer> getRetriesGetCaseUrl() {
        return retriesGetCaseUrl == null ? Collections.emptyList() : retriesGetCaseUrl;
    }

    public List<SearchAliasField> getSearchAliasFields() {
        return searchAliasFields;
    }

    public void setSearchAliasFields(List<SearchAliasField> searchAliasFields) {
        if (searchAliasFields != null) {
            this.searchAliasFields.addAll(searchAliasFields);
        }
    }

    @JsonIgnore
    public boolean isCaseFieldACollection(String caseFieldId) {
        return getCaseField(caseFieldId).map(CaseFieldDefinition::isCollectionFieldType).orElse(false);
    }

    @JsonIgnore
    public Optional<CaseFieldDefinition> getCaseField(String caseFieldId) {
        return caseFieldDefinitions.stream().filter(caseField ->
            caseField.getId().equalsIgnoreCase(caseFieldId)).findFirst();
    }

    @JsonIgnore
    public <T extends CommonField> Optional<T> getComplexSubfieldDefinitionByPath(String path) {
        return CaseFieldPathUtils.getFieldDefinitionByPath(this, path);
    }

    @JsonIgnore
    public Map<String, TextNode> getLabelsFromCaseFields() {
        return getCaseFieldDefinitions()
            .stream()
            .filter(caseField -> LABEL.equals(caseField.getFieldTypeDefinition().getType()))
            .collect(Collectors.toMap(CaseFieldDefinition::getId, caseField ->
                JsonNodeFactory.instance.textNode(caseField.getLabel())));
    }

    public List<RoleToAccessProfileDefinition> getRoleToAccessProfiles() {
        return roleToAccessProfiles;
    }

    public List<SearchParty> getSearchParties() {
        return searchParties;
    }

    public void setSearchParties(List<SearchParty> searchParties) {
        if (searchParties != null) {
            this.searchParties.addAll(searchParties);
        }
    }

    public List<SearchCriteria> getSearchCriterias() {
        return searchCriterias;
    }

    public void setSearchCriterias(List<SearchCriteria> searchCriterias) {
        if (searchCriterias != null) {
            this.searchCriterias.addAll(searchCriterias);
        }
    }

    public void setCategories(List<CategoryDefinition> categories) {
        if (categories != null) {
            this.categories.addAll(categories);
        }
    }

    public List<CategoryDefinition> getCategories() {
        return categories;
    }

    public static CaseTypeDefinitionBuilder caseTypeDefinitionCopy(CaseTypeDefinition caseType,
                                                                   List<CaseEventDefinition> events,
                                                                   List<CaseStateDefinition> states,
                                                                   List<CaseFieldDefinition> caseFieldDefinitions) {
        return CaseTypeDefinition.builder()
            .id(caseType.getId())
            .description(caseType.getDescription())
            .version(caseType.getVersion())
            .name(caseType.getName())
            .jurisdictionDefinition(caseType.getJurisdictionDefinition())
            .securityClassification(caseType.getSecurityClassification())
            .events(List.copyOf(events))
            .states(List.copyOf(states))
            .caseFieldDefinitions(List.copyOf(caseFieldDefinitions))
            .printableDocumentsUrl(caseType.getPrintableDocumentsUrl())
            .accessControlLists(List.copyOf(caseType.getAccessControlLists()))
            .callbackGetCaseUrl(caseType.getCallbackGetCaseUrl())
            .retriesGetCaseUrl(List.copyOf(caseType.getRetriesGetCaseUrl()))
            .searchAliasFields(List.copyOf(caseType.getSearchAliasFields()))
            .searchParties(List.copyOf(caseType.getSearchParties()))
            .searchCriterias(List.copyOf(caseType.getSearchCriterias()))
            .categories(List.copyOf(caseType.getCategories()))
            .roleToAccessProfiles(List.copyOf(caseType.getRoleToAccessProfiles()));
    }

    public static CaseTypeDefinitionBuilder caseTypeDefinitionCopy(CaseTypeDefinition caseType) {
        return caseTypeDefinitionCopy(caseType, caseType.getEvents(), caseType.getStates(),
            caseType.getCaseFieldDefinitions());
    }
}
