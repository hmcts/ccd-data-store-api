package uk.gov.hmcts.ccd.v2.external.resource;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.external.controller.DocumentController;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Data
@NoArgsConstructor
public class DocumentsResource extends ResourceSupport {

    List<Document> documentResources;

    public DocumentsResource(@NonNull String caseId, @NonNull List<Document> documents) {
        documentResources = documents;

        add(linkTo(methodOn(DocumentController.class).getDocuments(caseId, "ccd-data-store-api")).withSelfRel());
    }

}
