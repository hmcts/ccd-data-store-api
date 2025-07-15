package uk.gov.hmcts.ccd.v2.external.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DecentralisedAuditEvent {
    private Long id;
    private Long caseReference;
    private AuditEvent event;

    public AuditEvent getEvent() {
        this.event.setId(id);
        this.event.setCaseDataId(String.valueOf(caseReference));
        return event;
    }
}
