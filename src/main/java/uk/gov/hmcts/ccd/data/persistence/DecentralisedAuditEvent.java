package uk.gov.hmcts.ccd.data.persistence;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.Getter;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
class DecentralisedAuditEvent {
    private Long id;
    private Long caseReference;
    @Getter(lombok.AccessLevel.NONE)
    private AuditEvent event;

    public AuditEvent getEvent(String caseDataId) {
        this.event.setId(id);
        this.event.setCaseDataId(caseDataId);
        return event;
    }
}
