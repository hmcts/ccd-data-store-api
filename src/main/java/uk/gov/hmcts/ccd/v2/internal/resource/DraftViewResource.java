package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.v2.internal.controller.UIDraftsController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DraftViewResource extends RepresentationModel {
    private static final Logger LOG = LoggerFactory.getLogger(DraftViewResource.class);

    @JsonUnwrapped
    private DraftResponse draftResponse;

    public DraftViewResource(@NonNull DraftResponse draftResponse, String caseTypeId) {
        copyProperties(draftResponse);

        add(linkTo(methodOn(UIDraftsController.class).saveDraft(caseTypeId, draftResponse.getCaseDataContent()))
            .withSelfRel());
    }

    private void copyProperties(DraftResponse draftResponse) {
        this.draftResponse = draftResponse;
    }
}
