package uk.gov.hmcts.ccd.domain.model.lau;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CaseSearchPostResponse {

    private SearchLogPostResponse searchLog;
}
