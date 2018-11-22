package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.v2.internal.controller.UIStartTriggerController;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIStartTriggerResource extends ResourceSupport {

    @JsonProperty("id")
    private String eventId;
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

    public UIStartTriggerResource(@NonNull CaseEventTrigger caseEventTrigger, String caseTypeId, Boolean ignoreWarning) {
        copyProperties(caseEventTrigger);

        add(linkTo(methodOn(UIStartTriggerController.class).getStartTrigger(caseTypeId, caseEventTrigger.getId(), ignoreWarning)).withSelfRel());
    }

    private void copyProperties(CaseEventTrigger caseEventTrigger) {
        this.eventId = caseEventTrigger.getId();
        this.name = caseEventTrigger.getName();
        this.description = caseEventTrigger.getDescription();
        this.caseId = caseEventTrigger.getCaseId();
        this.caseFields = caseEventTrigger.getCaseFields();
        this.eventToken = caseEventTrigger.getEventToken();
        this.wizardPages = caseEventTrigger.getWizardPages();
        this.showSummary = caseEventTrigger.getShowSummary();
        this.showEventNotes = caseEventTrigger.getShowEventNotes();
        this.endButtonLabel = caseEventTrigger.getEndButtonLabel();
        this.canSaveDraft = caseEventTrigger.getCanSaveDraft();
    }
}
