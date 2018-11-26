package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.v2.internal.controller.UIStartTriggerController;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIStartTriggerResource extends ResourceSupport {

    @JsonUnwrapped
    private CaseEventTrigger caseEventTrigger;

    public UIStartTriggerResource(@NonNull CaseEventTrigger caseEventTrigger, String caseTypeId, Boolean ignoreWarning) {
        copyProperties(caseEventTrigger);

        add(linkTo(methodOn(UIStartTriggerController.class).getStartTrigger(caseTypeId, caseEventTrigger.getId(), ignoreWarning)).withSelfRel());
    }

    private void copyProperties(CaseEventTrigger caseEventTrigger) {
        this.caseEventTrigger = caseEventTrigger;
    }
}
