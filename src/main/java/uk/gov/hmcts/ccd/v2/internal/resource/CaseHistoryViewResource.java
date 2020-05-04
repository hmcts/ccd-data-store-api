package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseHistoryViewResource extends RepresentationModel {

    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_type")
    private CaseViewType caseType;
    private CaseViewTab[] tabs;
    private List<CaseViewField> metadataFields;
    @JsonProperty("event")
    private CaseViewEvent event;

    public CaseHistoryViewResource(@NonNull CaseHistoryView caseHistoryView, String caseId) {
        copyProperties(caseHistoryView);

        add(linkTo(methodOn(UICaseController.class).getCaseHistoryView(caseId, caseHistoryView.getEventId().toString())).withSelfRel());
    }

    private void copyProperties(CaseHistoryView caseViewEvent) {
        this.caseId = caseViewEvent.getCaseId();
        this.caseType = caseViewEvent.getCaseType();
        this.tabs = caseViewEvent.getTabs();
        this.metadataFields = caseViewEvent.getMetadataFields();
        this.event = caseViewEvent.getEvent();
    }
}
