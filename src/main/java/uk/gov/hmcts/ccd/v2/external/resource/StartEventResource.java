package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.controller.StartEventController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StartEventResource extends RepresentationModel<RepresentationModel<?>> {


    @JsonProperty("case_details")
    private CaseDetails caseDetails;
    @JsonProperty("event_id")
    private String eventId;
    private String token;

    public StartEventResource(@NonNull StartEventResult startEventResult, Boolean ignoreWarning, Boolean withCase) {
        copyProperties(startEventResult);

        if (withCase) {
            add(linkTo(methodOn(StartEventController.class).getStartEventTrigger(startEventResult.getCaseReference(), eventId, ignoreWarning)).withSelfRel());
        } else {
            add(linkTo(methodOn(StartEventController.class).getStartCaseEvent(startEventResult.getCaseTypeId(), eventId, ignoreWarning)).withSelfRel());
        }
    }

    private void copyProperties(StartEventResult startEventResult) {
        this.caseDetails = startEventResult.getCaseDetails();
        this.eventId = startEventResult.getEventId();
        this.token = startEventResult.getToken();
    }
}
