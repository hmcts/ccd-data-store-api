package uk.gov.hmcts.ccd.v2.external.resource;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.external.controller.DocumentController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@NoArgsConstructor
public class DocumentsResource extends RepresentationModel {

    List<Document> documentResources;

    public DocumentsResource(@NonNull String caseId, @NonNull List<Document> documents) {
        documentResources = documents;

        add(linkTo(methodOn(DocumentController.class).getDocuments(caseId)).withSelfRel());
    }

}
