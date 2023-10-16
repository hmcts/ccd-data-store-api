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
public class CaseEventDefinition implements Serializable, Copyable<CaseEventDefinition> {

    private String id = null;
    private String name = null;
    private String description = null;
    @JsonProperty("order")
    private Integer displayOrder = null;
    @JsonProperty("case_fields")
    private List<CaseEventFieldDefinition> caseFields = new ArrayList<>();
    @JsonProperty("pre_states")
    private List<String> preStates = new ArrayList<>();
    @JsonProperty("post_states")
    private List<EventPostStateDefinition> postStates = new ArrayList<>();
    @JsonProperty("callback_url_about_to_start_event")
    private String callBackURLAboutToStartEvent;
    @JsonProperty("retries_timeout_about_to_start_event")
    private List<Integer> retriesTimeoutAboutToStartEvent;
    @JsonProperty("callback_url_about_to_submit_event")
    private String callBackURLAboutToSubmitEvent;
    @JsonProperty("retries_timeout_url_about_to_submit_event")
    private List<Integer> retriesTimeoutURLAboutToSubmitEvent;
    @JsonProperty("callback_url_submitted_event")
    private String callBackURLSubmittedEvent;
    @JsonProperty("retries_timeout_url_submitted_event")
    private List<Integer> retriesTimeoutURLSubmittedEvent;
    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;
    @JsonProperty("show_summary")
    private Boolean showSummary = null;
    @JsonProperty("show_event_notes")
    private Boolean showEventNotes = null;
    @JsonProperty("end_button_label")
    private String endButtonLabel = null;
    @JsonProperty("can_save_draft")
    private Boolean canSaveDraft = null;
    @JsonProperty("publish")
    private Boolean publish;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    @JsonProperty("event_enabling_condition")
    private String eventEnablingCondition;
    @JsonProperty("ttl_increment")
    private Integer ttlIncrement;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public List<CaseEventFieldDefinition> getCaseFields() {
        return caseFields;
    }

    public void setCaseFields(List<CaseEventFieldDefinition> caseFields) {
        this.caseFields = caseFields;
    }

    public List<String> getPreStates() {
        return preStates;
    }

    public void setPreStates(List<String> preStates) {
        this.preStates = preStates;
    }

    public List<EventPostStateDefinition> getPostStates() {
        return postStates;
    }

    public void setPostStates(List<EventPostStateDefinition> postStates) {
        this.postStates = postStates;
    }

    public String getCallBackURLAboutToStartEvent() {
        return callBackURLAboutToStartEvent;
    }

    public void setCallBackURLAboutToStartEvent(String callBackURLAboutToStartEvent) {
        this.callBackURLAboutToStartEvent = callBackURLAboutToStartEvent;
    }

    public List<Integer> getRetriesTimeoutAboutToStartEvent() {
        return retriesTimeoutAboutToStartEvent;
    }

    public void setRetriesTimeoutAboutToStartEvent(List<Integer> retriesTimeoutAboutToStartEvent) {
        this.retriesTimeoutAboutToStartEvent = retriesTimeoutAboutToStartEvent;
    }

    public String getCallBackURLAboutToSubmitEvent() {
        return callBackURLAboutToSubmitEvent;
    }

    public void setCallBackURLAboutToSubmitEvent(String callBackURLAboutToSubmitEvent) {
        this.callBackURLAboutToSubmitEvent = callBackURLAboutToSubmitEvent;
    }

    public List<Integer> getRetriesTimeoutURLAboutToSubmitEvent() {
        return retriesTimeoutURLAboutToSubmitEvent;
    }

    public void setRetriesTimeoutURLAboutToSubmitEvent(List<Integer> retriesTimeoutURLAboutToSubmitEvent) {
        this.retriesTimeoutURLAboutToSubmitEvent = retriesTimeoutURLAboutToSubmitEvent;
    }

    public String getCallBackURLSubmittedEvent() {
        return callBackURLSubmittedEvent;
    }

    public void setCallBackURLSubmittedEvent(String callBackURLSubmittedEvent) {
        this.callBackURLSubmittedEvent = callBackURLSubmittedEvent;
    }

    public List<Integer> getRetriesTimeoutURLSubmittedEvent() {
        return retriesTimeoutURLSubmittedEvent;
    }

    public void setRetriesTimeoutURLSubmittedEvent(List<Integer> retriesTimeoutURLSubmittedEvent) {
        this.retriesTimeoutURLSubmittedEvent = retriesTimeoutURLSubmittedEvent;
    }

    public SecurityClassification getSecurityClassification() {
        return securityClassification;
    }

    public void setSecurityClassification(SecurityClassification securityClassification) {
        this.securityClassification = securityClassification;
    }


    public List<AccessControlList> getAccessControlLists() {
        return accessControlLists;
    }

    public void setAccessControlLists(List<AccessControlList> accessControlLists) {
        this.accessControlLists = accessControlLists;
    }

    public Boolean getShowSummary() {
        return showSummary;
    }

    public void setShowSummary(final Boolean showSummary) {
        this.showSummary = showSummary;
    }

    public Boolean getShowEventNotes() {
        return showEventNotes;
    }

    public void setShowEventNotes(Boolean showEventNotes) {
        this.showEventNotes = showEventNotes;
    }

    public String getEndButtonLabel() {
        return endButtonLabel;
    }

    public void setEndButtonLabel(String endButtonLabel) {
        this.endButtonLabel = endButtonLabel;
    }

    public Boolean getCanSaveDraft() {
        return canSaveDraft;
    }

    public void setCanSaveDraft(Boolean canSaveDraft) {
        this.canSaveDraft = canSaveDraft;
    }

    public Boolean getPublish() {
        return publish;
    }

    public void setPublish(Boolean publish) {
        this.publish = publish;
    }

    public Optional<CaseEventFieldDefinition> getCaseEventField(String caseFieldId) {
        return getCaseFields().stream()
            .filter(f -> f.getCaseFieldId().equals(caseFieldId))
            .findFirst();
    }

    public String getEventEnablingCondition() {
        return eventEnablingCondition;
    }

    public void setEventEnablingCondition(String eventEnablingCondition) {
        this.eventEnablingCondition = eventEnablingCondition;
    }

    public Integer getTtlIncrement() {
        return ttlIncrement;
    }

    public void setTtlIncrement(Integer ttlIncrement) {
        this.ttlIncrement = ttlIncrement;
    }

    @JsonIgnore
    @Override
    public CaseEventDefinition createCopy() {
        CaseEventDefinition copy = new CaseEventDefinition();
        copy.setId(this.getId());
        copy.setName(this.getName());
        copy.setDescription(this.getDescription());
        copy.setDisplayOrder(this.getDisplayOrder());
        copy.setCaseFields(createCopyList(this.getCaseFields()));
        copy.setPreStates(this.getPreStates() != null ? new ArrayList<>(this.getPreStates()) : null);
        copy.setPostStates(createCopyList(this.getPostStates()));
        copy.setRetriesTimeoutAboutToStartEvent(this.getRetriesTimeoutAboutToStartEvent() != null
            ? new ArrayList<>(this.getRetriesTimeoutAboutToStartEvent()) : null);
        copy.setRetriesTimeoutURLAboutToSubmitEvent(this.getRetriesTimeoutURLAboutToSubmitEvent() != null
            ? new ArrayList<>(this.getRetriesTimeoutURLAboutToSubmitEvent()) : null);
        copy.setRetriesTimeoutURLSubmittedEvent(this.getRetriesTimeoutURLSubmittedEvent() != null
            ? new ArrayList<>(this.getRetriesTimeoutURLSubmittedEvent()) : null);
        copy.setAccessControlLists(createACLCopyList(this.getAccessControlLists()));
        copy.setCallBackURLAboutToStartEvent(this.getCallBackURLAboutToStartEvent());
        copy.setCallBackURLAboutToSubmitEvent(this.getCallBackURLAboutToSubmitEvent());
        copy.setCallBackURLSubmittedEvent(this.getCallBackURLSubmittedEvent());
        copy.setSecurityClassification(this.getSecurityClassification());
        copy.setShowSummary(this.getShowSummary());
        copy.setShowEventNotes(this.getShowEventNotes());
        copy.setEndButtonLabel(this.getEndButtonLabel());
        copy.setCanSaveDraft(this.getCanSaveDraft());
        copy.setPublish(this.getPublish());
        copy.setEventEnablingCondition(this.getEventEnablingCondition());
        copy.setTtlIncrement(this.getTtlIncrement());

        return copy;
    }
}
