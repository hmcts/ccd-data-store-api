package uk.gov.hmcts.ccd.v2.internal.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseViewResource extends RepresentationModel {

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
    private CaseViewActionableEvent[] caseViewActionableEvents;

    @JsonProperty("events")
    private CaseViewEvent[] caseViewEvents;

    public CaseViewResource(@NonNull CaseView caseView) {
        copyProperties(caseView);

        add(linkTo(methodOn(UICaseController.class).getCaseView(reference)).withSelfRel());
    }

    private void copyProperties(CaseView caseView) {
        this.reference = caseView.getCaseId();
        this.caseType = caseView.getCaseType();
        this.tabs = caseView.getTabs();
        this.metadataFields = caseView.getMetadataFields();
        this.state = caseView.getState();
        this.caseViewActionableEvents = caseView.getActionableEvents();
        this.caseViewEvents = caseView.getEvents();
    }
}
