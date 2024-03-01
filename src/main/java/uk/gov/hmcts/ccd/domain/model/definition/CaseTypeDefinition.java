package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.ToString;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.CaseFieldPathUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.LABEL;

@ToString
public class CaseTypeDefinition implements Serializable, Copyable<CaseTypeDefinition> {
    private static final long serialVersionUID = 5688786015302840008L;
    private String id;
    private String description;
    private Version version;
    private String name;
    @JsonProperty("jurisdiction")
    private JurisdictionDefinition jurisdictionDefinition;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    private List<CaseEventDefinition> events = new ArrayList<>();
    private List<CaseStateDefinition> states = new ArrayList<>();
    @JsonProperty("case_fields")
    private List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
    @JsonProperty("printable_document_url")
    private String printableDocumentsUrl;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    @JsonProperty("callback_get_case_url")
    private String callbackGetCaseUrl;
    @JsonProperty("retries_get_case_url")
    private List<Integer> retriesGetCaseUrl;
    private final List<SearchAliasField> searchAliasFields = new ArrayList<>();
    private final List<SearchParty> searchParties = new ArrayList<>();
    private final List<SearchCriteria> searchCriterias = new ArrayList<>();
    private final List<CategoryDefinition> categories = new ArrayList<>();
    @JsonProperty("roleToAccessProfiles")
    private List<RoleToAccessProfileDefinition> roleToAccessProfiles = new ArrayList<>();
    @JsonProperty("accessTypes")
    private List<AccessTypeDefinition> accessTypeDefinitions = new ArrayList<>();
    @JsonProperty("accessTypeRoles")
    private List<AccessTypeRoleDefinition> accessTypeRoleDefinitions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public String getJurisdictionId() {
        return jurisdictionDefinition.getId();
    }

    public JurisdictionDefinition getJurisdictionDefinition() {
        return jurisdictionDefinition;
    }

    public void setJurisdictionDefinition(JurisdictionDefinition jurisdictionDefinition) {
        this.jurisdictionDefinition = jurisdictionDefinition;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public List<CaseEventDefinition> getEvents() {
        return events;
    }

    public void setEvents(List<CaseEventDefinition> events) {
        this.events = events;
    }

    public List<CaseStateDefinition> getStates() {
        return states;
    }

    public void setStates(List<CaseStateDefinition> states) {
        this.states = states;
    }

    public List<CaseFieldDefinition> getCaseFieldDefinitions() {
        return caseFieldDefinitions;
    }

    public void setCaseFieldDefinitions(List<CaseFieldDefinition> caseFieldDefinitions) {
        this.caseFieldDefinitions = caseFieldDefinitions;
    }

    public String getPrintableDocumentsUrl() {
        return printableDocumentsUrl;
    }

    public void setPrintableDocumentsUrl(String printableDocumentsUrl) {
        this.printableDocumentsUrl = printableDocumentsUrl;
    }

    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
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

    public void setCallbackGetCaseUrl(String callbackGetCaseUrl) {
        this.callbackGetCaseUrl = callbackGetCaseUrl;
    }

    public List<Integer> getRetriesGetCaseUrl() {
        return retriesGetCaseUrl == null ? Collections.emptyList() : retriesGetCaseUrl;
    }

    public void setRetriesGetCaseUrl(List<Integer> retriesGetCaseUrl) {
        this.retriesGetCaseUrl = retriesGetCaseUrl;
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

    public void setRoleToAccessProfiles(List<RoleToAccessProfileDefinition> roleToAccessProfiles) {
        this.roleToAccessProfiles = roleToAccessProfiles;
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

    public void setAccessTypeRoleDefinitions(List<AccessTypeRoleDefinition> accessTypeRoles) {
        if (accessTypeRoles != null && !accessTypeRoles.isEmpty()) {
            this.accessTypeRoleDefinitions.addAll(accessTypeRoles);
        }
    }

    public List<AccessTypeRoleDefinition> getAccessTypeRoleDefinitions() {
        return accessTypeRoleDefinitions;
    }

    public void setAccessTypeDefinitions(List<AccessTypeDefinition> accessTypes) {
        if (accessTypes != null && !accessTypes.isEmpty()) {
            this.accessTypeDefinitions.addAll(accessTypes);
        }
    }

    public List<AccessTypeDefinition> getAccessTypeDefinitions() {
        return accessTypeDefinitions;
    }

    @JsonIgnore
    @Override
    public CaseTypeDefinition createCopy() {
        CaseTypeDefinition copy = new CaseTypeDefinition();
        copy.setId(this.getId());
        copy.setDescription(this.getDescription());
        copy.setVersion(this.getVersion() != null ? this.getVersion().createCopy() : null);
        copy.setName(this.getName());
        copy.setJurisdictionDefinition(this.getJurisdictionDefinition() != null
            ? this.getJurisdictionDefinition().createCopy() : null);
        copy.setSecurityClassification(this.getSecurityClassification());
        copy.setEvents(createCopyList(this.getEvents()));
        copy.setStates(createCopyList(this.getStates()));
        copy.setCaseFieldDefinitions(createCopyList(this.getCaseFieldDefinitions()));
        copy.setPrintableDocumentsUrl(this.getPrintableDocumentsUrl());
        copy.setAccessControlLists(createACLCopyList(this.getAccessControlLists()));
        copy.setCallbackGetCaseUrl(this.getCallbackGetCaseUrl());
        copy.setRetriesGetCaseUrl(this.getRetriesGetCaseUrl() != null
            ? new ArrayList<>(this.getRetriesGetCaseUrl()) : null);
        copy.setSearchAliasFields(createCopyList(this.getSearchAliasFields()));
        copy.setSearchParties(createCopyList(this.getSearchParties()));
        copy.setSearchCriterias(createCopyList(this.getSearchCriterias()));
        copy.setCategories(createCopyList(this.getCategories()));
        copy.setRoleToAccessProfiles(createCopyList(this.getRoleToAccessProfiles()));
        copy.setAccessTypeRoleDefinitions(createCopyList(this.getAccessTypeRoleDefinitions()));
        copy.setAccessTypeDefinitions(createCopyList(this.getAccessTypeDefinitions()));

        return copy;
    }
}
