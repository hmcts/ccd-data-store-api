package uk.gov.hmcts.ccd.domain.model.migration;

import lombok.Data;

@Data
public class MigrationResult {

    private int recordCount;
    private int finalRecordId;

}
