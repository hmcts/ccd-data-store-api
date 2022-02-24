package uk.gov.hmcts.ccd.domain.model.caselinks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MigrationParameters {

    private String caseTypeId;
    private String jurisdictionId;
    private Long caseDataId;
    private Integer numRecords;

}
