package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public class CaseUpdateViewEvent {
    private String id;
    private String name;
    private String description;
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_fields")
    private List<CaseViewField> caseFields;
    @JsonProperty("event_token")
    private String eventToken;
    @JsonProperty("wizard_pages")
    private List<WizardPage> wizardPages;
    @JsonProperty("show_summary")
    private Boolean showSummary;
    @JsonProperty("show_event_notes")
    private Boolean showEventNotes;
    @JsonProperty("end_button_label")
    private String endButtonLabel;
    @JsonProperty("can_save_draft")
    private Boolean canSaveDraft;
    @JsonInclude(NON_NULL)
    @JsonProperty("access_granted")
    private String accessGrants;
    @JsonInclude(NON_NULL)
    @JsonProperty("access_process")
    private String accessProcess;
    @JsonProperty("title_display")
    private String titleDisplay;
    @JsonProperty("supplementary_data")
    private Map<String, JsonNode> supplementaryData;

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

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public List<CaseViewField> getCaseFields() {
        return caseFields;
    }

    public void setCaseFields(List<CaseViewField> caseFields) {
        this.caseFields = caseFields;
    }

    public String getEventToken() {
        return eventToken;
    }

    public void setEventToken(String eventToken) {
        this.eventToken = eventToken;
    }

    public List<WizardPage> getWizardPages() {
        return wizardPages;
    }

    public void setWizardPages(List<WizardPage> wizardPages) {
        this.wizardPages = wizardPages;
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

    public String getAccessGrants() {
        return accessGrants;
    }

    public void setAccessGrants(String accessGrants) {
        this.accessGrants = accessGrants;
    }

    public String getAccessProcess() {
        return accessProcess;
    }

    public void setAccessProcess(String accessProcess) {
        this.accessProcess = accessProcess;
    }

    public String getTitleDisplay() {
        return titleDisplay;
    }

    public void setTitleDisplay(String titleDisplay) {
        this.titleDisplay = titleDisplay;
    }

    public Map<String, JsonNode> getSupplementaryData() {
        return supplementaryData;
    }

    public void setSupplementaryData(Map<String, JsonNode> supplementaryData) {
        this.supplementaryData = supplementaryData;
    }
}
