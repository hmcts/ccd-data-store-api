package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.v2.external.controller.CaseDocumentController;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseDocumentResource extends RepresentationModel<CaseDocumentResource> {

    @JsonProperty("documentMetadata")
    private CaseDocumentMetadata documentMetadata;

    public CaseDocumentResource(@NonNull String caseId,
                                @NonNull String documentId,
                                CaseDocumentMetadata documentMetadata) {
        this.documentMetadata = documentMetadata;
        add(linkTo(methodOn(CaseDocumentController.class).getCaseDocumentMetadata(caseId, documentId)).withSelfRel());
    }
}
