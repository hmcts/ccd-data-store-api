package uk.gov.hmcts.ccd.domain.model.caselinking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reason {
    private String reasonCode;
    private String otherDescription;
}
