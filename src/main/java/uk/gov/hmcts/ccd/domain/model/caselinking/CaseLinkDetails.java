package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseLinkDetails {
    private LocalDateTime createdDateTime;
    private List<Reason> reasons;
}
