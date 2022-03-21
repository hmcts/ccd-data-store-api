package uk.gov.hmcts.ccd.domain.model.globalsearch;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SearchParty {
    private String id;
    private SearchPartyValue value;
}
