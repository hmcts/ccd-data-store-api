package uk.gov.hmcts.ccd.domain.model.search.global;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SearchCriteriaResponseTEMP {

    private SearchCriteria requestValues;

    private List<SearchCriteriaResponse> response;

}
