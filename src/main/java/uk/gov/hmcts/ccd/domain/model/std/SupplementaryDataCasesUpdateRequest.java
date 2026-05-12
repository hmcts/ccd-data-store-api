package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SupplementaryDataCasesUpdateRequest {

    @JsonProperty("cases")
    private List<String> caseIds;

    @JsonProperty("supplementary_data_updates")
    private Map<String, Map<String, Object>> requestData;

}

