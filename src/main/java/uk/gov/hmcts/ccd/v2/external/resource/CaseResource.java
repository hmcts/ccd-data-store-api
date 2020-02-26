package uk.gov.hmcts.ccd.v2.external.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseResource extends RepresentationModel {

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

    @JsonProperty("last_state_modified_on")
    private LocalDateTime lastStateModifiedOn;

    @JsonProperty("state")
    private String state;

    @JsonProperty("security_classification")
    private SecurityClassification securityClassification;

    @JsonProperty("data")
    private Map<String, JsonNode> data;

    @JsonProperty("data_classification")
    private Map<String, JsonNode> dataClassification;

    @JsonProperty("after_submit_callback_response")
    @SuppressWarnings("squid:common-java:DuplicatedBlocks")
    private AfterSubmitCallbackResponse afterSubmitCallbackResponse;

    @JsonProperty("callback_response_status_code")
    @SuppressWarnings("squid:common-java:DuplicatedBlocks")
    private Integer callbackResponseStatusCode;

    @JsonProperty("callback_response_status")
    @SuppressWarnings("squid:common-java:DuplicatedBlocks")
    private String callbackResponseStatus;

    @JsonProperty("delete_draft_response_status_code")
    @SuppressWarnings("squid:common-java:DuplicatedBlocks")
    private Integer deleteDraftResponseStatusCode;

    @JsonProperty("delete_draft_response_status")
    @SuppressWarnings("squid:common-java:DuplicatedBlocks")
    private String deleteDraftResponseStatus;


    public CaseResource(@NonNull CaseDetails caseDetails) {
        copyProperties(caseDetails);

        add(linkTo(methodOn(CaseController.class).getCase(reference)).withSelfRel());
    }

    public CaseResource(@NonNull CaseDetails caseDetails, @NotNull CaseDataContent caseDataContent) {
        copyProperties(caseDetails);

        add(linkTo(methodOn(CaseController.class).createEvent(reference, caseDataContent)).withSelfRel());
    }

    public CaseResource(@NonNull CaseDetails caseDetails, @NotNull CaseDataContent caseDataContent, Boolean ignoreWarning) {
        copyProperties(caseDetails);

        add(linkTo(methodOn(CaseController.class).createCase(caseType, caseDataContent, ignoreWarning)).withSelfRel());
    }

    private void copyProperties(CaseDetails caseDetails) {
        this.reference = caseDetails.getReference().toString();
        this.jurisdiction = caseDetails.getJurisdiction();
        this.caseType = caseDetails.getCaseTypeId();
        this.createdOn = caseDetails.getCreatedDate();
        this.lastModifiedOn = caseDetails.getLastModified();
        this.lastStateModifiedOn = caseDetails.getLastStateModifiedDate();
        this.state = caseDetails.getState();
        this.securityClassification = caseDetails.getSecurityClassification();
        this.data = caseDetails.getData();
        this.dataClassification = caseDetails.getDataClassification();
        this.afterSubmitCallbackResponse = caseDetails.getAfterSubmitCallbackResponse();
        this.callbackResponseStatusCode = caseDetails.getCallbackResponseStatusCode();
        this.callbackResponseStatus = caseDetails.getCallbackResponseStatus();
        this.deleteDraftResponseStatusCode = caseDetails.getDeleteDraftResponseStatusCode();
        this.deleteDraftResponseStatus = caseDetails.getDeleteDraftResponseStatus();
    }
}
