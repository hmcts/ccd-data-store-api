package uk.gov.hmcts.ccd.domain.model.lau;

import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Setter
public class ActionLog implements Serializable {

    public static final long serialVersionUID = 432973322;

    private String userId;
    private String caseAction;
    private String caseRef;
    private String caseJurisdictionId;
    private String caseTypeId;
    private String timestamp;

    public ActionLog(final String userId,
        final String caseAction,
        final String caseRef,
        final String caseJurisdictionId,
        final String caseTypeId,
        final String timestamp) {
        this.userId = userId;
        this.caseAction = caseAction;
        this.caseRef = caseRef;
        this.caseJurisdictionId = caseJurisdictionId;
        this.caseTypeId = caseTypeId;
        this.timestamp = timestamp;
    }

}
