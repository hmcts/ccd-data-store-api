package uk.gov.hmcts.ccd.domain.model.lau;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchLogPostResponse {

    private String id;
    private String userId;
    private List<String> caseRefs;
    private String timestamp;
}
