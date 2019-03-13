package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.v2.internal.controller.UIStartTriggerController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIStartTriggerResource extends ResourceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(UIStartTriggerResource.class);

    private enum Origin { DRAFT, CASE, CASE_TYPE }

    @JsonUnwrapped
    private CaseEventTrigger caseEventTrigger;

    public static UIStartTriggerResource forCase(@NonNull CaseEventTrigger caseEventTrigger, String caseId, Boolean ignoreWarning) {
        return new UIStartTriggerResource(caseEventTrigger, caseId, ignoreWarning, Origin.CASE);
    }

    public static UIStartTriggerResource forCaseType(@NonNull CaseEventTrigger caseEventTrigger, String caseType, Boolean ignoreWarning) {
        return new UIStartTriggerResource(caseEventTrigger, caseType, ignoreWarning, Origin.CASE_TYPE);
    }

    public static UIStartTriggerResource forDraft(@NonNull CaseEventTrigger caseEventTrigger, String draftId, Boolean ignoreWarning) {
        return new UIStartTriggerResource(caseEventTrigger, draftId, ignoreWarning, Origin.DRAFT);
    }

    private UIStartTriggerResource(@NonNull CaseEventTrigger caseEventTrigger, String id, Boolean ignoreWarning, Origin origin) {
        copyProperties(caseEventTrigger);

        switch (origin) {
            case CASE_TYPE:
                add(linkTo(methodOn(UIStartTriggerController.class).getStartCaseTrigger(id, caseEventTrigger.getId(), ignoreWarning)).withSelfRel());
                break;
            case CASE:
                add(linkTo(methodOn(UIStartTriggerController.class).getStartEventTrigger(id, caseEventTrigger.getId(), ignoreWarning)).withSelfRel());
                break;
            case DRAFT:
                add(linkTo(methodOn(UIStartTriggerController.class).getStartDraftTrigger(id, ignoreWarning)).withSelfRel());
                break;
            default:
                LOG.warn("Origin={} not supported", origin);
                break;
        }
    }

    private void copyProperties(CaseEventTrigger caseEventTrigger) {
        this.caseEventTrigger = caseEventTrigger;
    }
}
