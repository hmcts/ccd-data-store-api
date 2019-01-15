package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.ResourceSupport;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UIEventViewResource extends ResourceSupport {

    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_type")
    private CaseViewType caseType;
    private CaseViewTab[] tabs;
    private List<CaseViewField> metadataFields;
    @JsonProperty("event")
    private CaseViewEvent event;

    public UIEventViewResource(@NonNull CaseHistoryView caseHistoryView, String caseId) {
        copyProperties(caseHistoryView);

        add(linkTo(methodOn(UICaseController.class).getCaseEvent(caseId, caseHistoryView.getEventId().toString())).withSelfRel());
    }

    private void copyProperties(CaseHistoryView caseViewEvent) {
        this.caseId = caseViewEvent.getCaseId();
        this.caseType = caseViewEvent.getCaseType();
        this.tabs = caseViewEvent.getTabs();
        this.metadataFields = caseViewEvent.getMetadataFields();
        this.event = caseViewEvent.getEvent();
    }
}
