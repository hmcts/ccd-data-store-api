package uk.gov.hmcts.ccd.domain.model.std;

import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidSortBy;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidSortDirection;

@Getter
@Setter
public class SortCriteria {

    @ValidSortBy
    public String sortBy;

    @ValidSortDirection
    public String sortDirection;


}
