package uk.gov.hmcts.ccd.v2.external.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.external.controller.DocumentController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class DocumentsResource extends RepresentationModel {

    List<Document> documentResources;

    public DocumentsResource(@NonNull String caseId, @NonNull List<Document> documents) {
        documentResources = documents;

        add(linkTo(methodOn(DocumentController.class).getDocuments(caseId)).withSelfRel());
    }

}
