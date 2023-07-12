package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Value
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PACKAGE)
@SuppressWarnings("RedundantModifiersValueLombok") // see https://sonarsource.atlassian.net/browse/SONARJAVA-4536
public class CaseEventDefinition implements Serializable {

    String id;
    String name;
    String description;
    @JsonProperty("order")
    Integer displayOrder;
    @JsonProperty("case_fields")
    @Builder.Default
    private List<CaseEventFieldDefinition> caseFields = new ArrayList<>();
    @JsonProperty("pre_states")
    @Builder.Default
    private List<String> preStates = new ArrayList<>();
    @JsonProperty("post_states")
    @Builder.Default
    private List<EventPostStateDefinition> postStates = new ArrayList<>();
    @JsonProperty("callback_url_about_to_start_event")
    private String callBackURLAboutToStartEvent;
    @JsonProperty("retries_timeout_about_to_start_event")
    private List<Integer> retriesTimeoutAboutToStartEvent;
    @JsonProperty("callback_url_about_to_submit_event")
    String callBackURLAboutToSubmitEvent;
    @JsonProperty("retries_timeout_url_about_to_submit_event")
    private List<Integer> retriesTimeoutURLAboutToSubmitEvent;
    @JsonProperty("callback_url_submitted_event")
    String callBackURLSubmittedEvent;
    @JsonProperty("retries_timeout_url_submitted_event")
    private List<Integer> retriesTimeoutURLSubmittedEvent;
    @JsonProperty("security_classification")
    SecurityClassification securityClassification;
    @JsonProperty("show_summary")
    Boolean showSummary;
    @JsonProperty("show_event_notes")
    Boolean showEventNotes;
    @JsonProperty("end_button_label")
    String endButtonLabel;
    @JsonProperty("can_save_draft")
    Boolean canSaveDraft;
    @JsonProperty("publish")
    Boolean publish;
    @JsonProperty("acls")
    private List<AccessControlList> accessControlLists;
    @JsonProperty("event_enabling_condition")
    String eventEnablingCondition;
    @JsonProperty("ttl_increment")
    Integer ttlIncrement;

    public Optional<CaseEventFieldDefinition> getCaseEventField(String caseFieldId) {
        return getCaseFields().stream()
            .filter(f -> f.getCaseFieldId().equals(caseFieldId))
            .findFirst();
    }
}
