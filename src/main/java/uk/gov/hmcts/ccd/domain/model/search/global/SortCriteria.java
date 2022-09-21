package uk.gov.hmcts.ccd.domain.model.search.global;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch.ValidSortBy;
import uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch.ValidSortDirection;

@Getter
@Setter
public class SortCriteria {

    @ValidSortBy
    private String sortBy;

    @ValidSortDirection
    private String sortDirection;


}
