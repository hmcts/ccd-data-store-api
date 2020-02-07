package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UICaseViewResource extends RepresentationModel {

    @JsonProperty("case_id")
    private String reference;

    @JsonProperty("case_type")
    private CaseViewType caseType;

    @JsonProperty("tabs")
    private CaseViewTab[] tabs;

    @JsonProperty("metadataFields")
    private List<CaseViewField> metadataFields;

    @JsonProperty("state")
    private ProfileCaseState state;

    @JsonProperty("triggers")
    private CaseViewTrigger[] triggers;

    @JsonProperty("events")
    private CaseViewEvent[] events;

    public UICaseViewResource(@NonNull CaseView caseView) {
        copyProperties(caseView);

        add(linkTo(methodOn(UICaseController.class).getCase(reference)).withSelfRel());
    }

    private void copyProperties(CaseView caseView) {
        this.reference = caseView.getCaseId();
        this.caseType = caseView.getCaseType();
        this.tabs = caseView.getTabs();
        this.metadataFields = caseView.getMetadataFields();
        this.state = caseView.getState();
        this.triggers = caseView.getTriggers();
        this.events = caseView.getEvents();
    }
}
