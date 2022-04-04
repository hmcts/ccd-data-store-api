package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
class CaseLinkDetails {
    private LocalDateTime createdDateTime;
    private List<Reason> reasons;
}
