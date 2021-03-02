package uk.gov.hmcts.ccd.v2.internal.resource;

import static org.springframework.hateoas.server.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.ControllerLinkBuilder.methodOn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.RepresentationModel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.v2.internal.controller.UIStartTriggerController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseUpdateViewEventResource extends RepresentationModel {
    private static final Logger LOG = LoggerFactory.getLogger(CaseUpdateViewEventResource.class);

    private enum Origin { DRAFT, CASE, CASE_TYPE }

    @JsonUnwrapped
    private CaseUpdateViewEvent caseUpdateViewEvent;

    public static CaseUpdateViewEventResource forCase(@NonNull CaseUpdateViewEvent caseUpdateViewEvent, String caseId,
                                                      Boolean ignoreWarning) {
        return new CaseUpdateViewEventResource(caseUpdateViewEvent, caseId, ignoreWarning, Origin.CASE);
    }

    public static CaseUpdateViewEventResource forCaseType(@NonNull CaseUpdateViewEvent caseUpdateViewEvent,
                                                          String caseType, Boolean ignoreWarning) {
        return new CaseUpdateViewEventResource(caseUpdateViewEvent, caseType, ignoreWarning, Origin.CASE_TYPE);
    }

    public static CaseUpdateViewEventResource forDraft(@NonNull CaseUpdateViewEvent caseUpdateViewEvent, String draftId,
                                                       Boolean ignoreWarning) {
        return new CaseUpdateViewEventResource(caseUpdateViewEvent, draftId, ignoreWarning, Origin.DRAFT);
    }

    @JsonIgnore
    public CaseUpdateViewEvent getCaseUpdateViewEvent() {
        return caseUpdateViewEvent;
    }

    private CaseUpdateViewEventResource(@NonNull CaseUpdateViewEvent caseUpdateViewEvent, String id,
                                        Boolean ignoreWarning, Origin origin) {
        copyProperties(caseUpdateViewEvent);

        switch (origin) {
            case CASE_TYPE:
                add(linkTo(methodOn(UIStartTriggerController.class).getCaseUpdateViewEventByCaseType(id,
                    caseUpdateViewEvent.getId(),
                    ignoreWarning)).withSelfRel());
                break;
            case CASE:
                add(linkTo(methodOn(UIStartTriggerController.class).getCaseUpdateViewEvent(id,
                    caseUpdateViewEvent.getId(), ignoreWarning)).withSelfRel());
                break;
            case DRAFT:
                add(linkTo(methodOn(UIStartTriggerController.class).getStartDraftTrigger(id, ignoreWarning))
                    .withSelfRel());
                break;
            default:
                LOG.warn("Origin={} not supported", origin);
                break;
        }
    }

    private void copyProperties(CaseUpdateViewEvent caseUpdateViewEvent) {
        this.caseUpdateViewEvent = caseUpdateViewEvent;
    }
}
