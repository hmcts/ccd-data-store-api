package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.RepresentationModel;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.external.controller.DocumentController;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class DocumentsResource extends RepresentationModel<RepresentationModel<?>> {

    List<Document> documentResources;

    public DocumentsResource(@NonNull String caseId, @NonNull List<Document> documents) {
        documentResources = documents;

        add(linkTo(methodOn(DocumentController.class).getDocuments(caseId)).withSelfRel());
    }

}
