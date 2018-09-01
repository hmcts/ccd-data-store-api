package uk.gov.hmcts.ccd.v2.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.controller.CaseController;

import java.time.LocalDateTime;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseResource extends ResourceSupport {

    @JsonProperty("id")
    private String reference;

    @JsonProperty("jurisdiction")
    private String jurisdiction;

    @JsonProperty("case_type")
    private String caseType;

    @JsonProperty("created_on")
    private LocalDateTime createdOn;

    @JsonProperty("last_modified_on")
    private LocalDateTime lastModifiedOn;

    @JsonProperty("state")
    private String state;

    public CaseResource(@NonNull CaseDetails caseDetails) {
        copyProperties(caseDetails);

        add(linkTo(methodOn(CaseController.class).getCase(reference)).withSelfRel());
    }

    private void copyProperties(CaseDetails caseDetails) {
        this.reference = caseDetails.getReference().toString();
        this.jurisdiction = caseDetails.getJurisdiction();
        this.caseType = caseDetails.getCaseTypeId();
        this.createdOn = caseDetails.getCreatedDate();
        this.lastModifiedOn = caseDetails.getLastModified();
        this.state = caseDetails.getState();
    }
}
