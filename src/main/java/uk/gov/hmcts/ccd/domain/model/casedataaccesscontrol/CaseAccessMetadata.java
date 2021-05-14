package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;

@Data
public class CaseAccessMetadata {

    public static final String ACCESS_GRANTED = "[ACCESS_GRANTED]";
    public static final String ACCESS_PROCESS = "[ACCESS_PROCESS]";

    public static final String ACCESS_GRANTED_LABEL = "Access Granted";

    private String accessGrants;
    private String accessProcess;
}
