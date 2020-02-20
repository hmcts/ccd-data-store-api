package uk.gov.hmcts.ccd.v2.internal.resource;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UICaseEventsResource extends RepresentationModel {

    private List<AuditEvent> auditEvents;

    public UICaseEventsResource(@NonNull String caseReference, @NonNull List<AuditEvent> listOfAuditEvents) {
        this.auditEvents = listOfAuditEvents;
        add(linkTo(methodOn(UICaseController.class).getCaseEvents(caseReference)).withSelfRel());
    }
}
