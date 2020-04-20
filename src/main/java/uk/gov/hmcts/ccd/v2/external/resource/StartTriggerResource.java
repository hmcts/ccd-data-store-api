package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.controller.StartTriggerController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StartTriggerResource extends RepresentationModel {

    @JsonProperty("case_details")
    private CaseDetails caseDetails;
    @JsonProperty("event_id")
    private String eventId;
    private String token;

    public StartTriggerResource(@NonNull StartEventTrigger startEventTrigger, Boolean ignoreWarning, Boolean withCase) {
        copyProperties(startEventTrigger);

        if (withCase) {
            add(linkTo(methodOn(StartTriggerController.class).getStartEventTrigger(startEventTrigger.getCaseReference(), eventId, ignoreWarning)).withSelfRel());
        } else {
            add(linkTo(methodOn(StartTriggerController.class).getStartCaseTrigger(startEventTrigger.getCaseTypeId(), eventId, ignoreWarning)).withSelfRel());
        }
    }

    private void copyProperties(StartEventTrigger startEventTrigger) {
        this.caseDetails = startEventTrigger.getCaseDetails();
        this.eventId = startEventTrigger.getEventId();
        this.token = startEventTrigger.getToken();
    }
}
