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
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDraftsController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIDraftResource extends ResourceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(UIStartTriggerResource.class);

    @JsonUnwrapped
    private DraftResponse draftResponse;

    public UIDraftResource(@NonNull DraftResponse draftResponse, String caseTypeId) {
        copyProperties(draftResponse);

        add(linkTo(methodOn(UIDraftsController.class).saveDraft(caseTypeId, draftResponse.getCaseDataContent())).withSelfRel());
    }

    private void copyProperties(DraftResponse draftResponse) {
        this.draftResponse = draftResponse;
    }
}
