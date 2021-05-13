package uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol;

import lombok.Data;

@Data
public class CaseAccessMetadata {
    private String accessGrants;
    private String accessProcess;
}
