package uk.gov.hmcts.ccd.domain.model.search.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;

@Getter
@Setter
public class SearchCriteriaResponseTEMP {

    @JsonProperty("requestValues")
    private SearchCriteria requestValues;
    @JsonProperty("response")
    private List<CaseDetails> response;

}
