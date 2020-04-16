package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ToString
public class CaseTypeDefinition implements Serializable {
    private static final long serialVersionUID = 5688786015302840008L;
    private String id;
    private String description;
    private Version version;
    private String name;
    private Jurisdiction jurisdiction;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    private List<CaseEvent> events = new ArrayList<>();
    private List<CaseStateDefinition> states = new ArrayList<>();
    @JsonProperty("case_fields")
    private List<CaseFieldDefinition> caseFieldDefinitions = new ArrayList<>();
    @JsonProperty("printable_document_url")
    private String printableDocumentsUrl;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    private final List<SearchAliasField> searchAliasFields = new ArrayList<>();

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
        return jurisdiction.getId();
    }

    public Jurisdiction getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(Jurisdiction jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }

    public List<CaseEvent> getEvents() {
        return events;
    }

    public void setEvents(List<CaseEvent> events) {
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
            .orElseThrow(() -> new RuntimeException(String.format("CaseFieldId %s not found in CaseType %s", fieldId, name)))
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

    public Optional<CaseEvent> findCaseEvent(String eventId) {
        return events.stream()
            .filter(event -> event.getId().equalsIgnoreCase(eventId))
            .findFirst();
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
        return caseFieldDefinitions.stream().filter(caseField -> caseField.getId().equalsIgnoreCase(caseFieldId)).findFirst();
    }
}
