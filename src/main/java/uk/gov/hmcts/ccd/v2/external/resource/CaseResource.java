package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

import java.time.LocalDateTime;
import java.util.Map;

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

    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;

    @JsonProperty("data")
    private Map<String, JsonNode> data;

    @JsonProperty("data_classification")
    private Map<String, JsonNode> dataClassification;

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
        this.securityClassification = caseDetails.getSecurityClassification();
        this.data = caseDetails.getData();
        this.dataClassification = caseDetails.getDataClassification();
    }
}
