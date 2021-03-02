package uk.gov.hmcts.ccd.v2.external.resource;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.RepresentationModel;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.v2.external.controller.CaseController;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseEventsResource extends RepresentationModel<RepresentationModel<?>> {

    private List<AuditEvent> auditEvents;

    public CaseEventsResource(@NonNull String caseReference, @NonNull List<AuditEvent> listOfAuditEvents) {
        this.auditEvents = listOfAuditEvents;
        add(linkTo(methodOn(CaseController.class).getCaseEvents(caseReference)).withSelfRel());
    }
}
