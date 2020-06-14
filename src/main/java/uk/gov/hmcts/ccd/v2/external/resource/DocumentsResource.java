package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.RepresentationModel;

import java.util.List;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.external.controller.DocumentController;

@NoArgsConstructor
public class DocumentsResource extends RepresentationModel<RepresentationModel<?>> {

    List<Document> documentResources;

    public DocumentsResource(@NonNull String caseId, @NonNull List<Document> documents) {
        documentResources = documents;

        add(linkTo(methodOn(DocumentController.class).getDocuments(caseId)).withSelfRel());
    }

}
