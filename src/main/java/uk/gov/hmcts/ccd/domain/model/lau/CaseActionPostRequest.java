package uk.gov.hmcts.ccd.domain.model.lau;

import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@NoArgsConstructor
@Setter
public class CaseActionPostRequest implements Serializable {

    public static final long serialVersionUID = 432973322;

    private ActionLog actionLog;

    public CaseActionPostRequest(final ActionLog actionLog) {
        this.actionLog = actionLog;
    }

}
