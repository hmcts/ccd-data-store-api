package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class Reason {
    private String reasonCode;
    private String otherDescription;
}
