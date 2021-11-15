package uk.gov.hmcts.ccd.domain.model.lau;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.ZonedDateTime;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

@NoArgsConstructor
@Setter
@Getter
public class ActionLog implements Serializable {

    public static final long serialVersionUID = 432973322;

    private String userId;
    private String caseAction;
    private String caseRef;
    private String caseJurisdictionId;
    private String caseTypeId;
    private ZonedDateTime timestamp;

    public ActionLog(final String userId,
        final String caseAction,
        final String caseRef,
        final String caseJurisdictionId,
        final String caseTypeId,
        final ZonedDateTime timestamp) {
        this.userId = userId;
        this.caseAction = caseAction;
        this.caseRef = caseRef;
        this.caseJurisdictionId = caseJurisdictionId;
        this.caseTypeId = caseTypeId;
        this.timestamp = timestamp;
    }

    public String getTimestamp() {
        return timestamp.format(ISO_INSTANT);
    }

}
