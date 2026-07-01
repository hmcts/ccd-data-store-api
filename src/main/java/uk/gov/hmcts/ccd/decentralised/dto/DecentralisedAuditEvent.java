package uk.gov.hmcts.ccd.decentralised.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class DecentralisedAuditEvent {
    private Long id;
    private Long caseReference;
    private AuditEvent event;

    public AuditEvent getEvent(String caseDataId) {
        this.event.setId(id);
        this.event.setCaseDataId(caseDataId);
        return event;
    }
}
