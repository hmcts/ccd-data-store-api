package uk.gov.hmcts.ccd.domain.model.globalsearch;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class OtherCaseReference {
    private String id;
    private String value;
}
