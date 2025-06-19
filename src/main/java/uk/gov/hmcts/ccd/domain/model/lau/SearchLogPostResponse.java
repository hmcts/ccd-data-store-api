package uk.gov.hmcts.ccd.domain.model.lau;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SearchLogPostResponse {
    private String id;
    private String userId;
    private List<String> caseRefs;
    private String timestamp;
}
