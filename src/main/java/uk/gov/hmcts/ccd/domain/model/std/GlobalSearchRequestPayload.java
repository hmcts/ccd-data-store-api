package uk.gov.hmcts.ccd.domain.model.std;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Getter
@Setter
public class GlobalSearchRequestPayload {

    @Range(max = 1000, min = 0, message = ValidationError.MAX_RECORD_COUNT_INVALID)
    public Integer maxReturnRecordCount;

    public Integer startRecordNumber;

    @Valid
    public SortCriteria sortCriteria;

    @NotNull(message = ValidationError.SEARCH_CRITERIA_MISSING)
    @Valid
    public SearchCriteria searchCriteria;

    @JsonIgnore
    public GlobalSearchRequestPayload setDefaults() {

        if (this.getMaxReturnRecordCount() == null) {
            this.setMaxReturnRecordCount(25);
        }
        if (this.getStartRecordNumber() == null) {
            this.setStartRecordNumber(1);
        }
        if (this.getSortCriteria() == null) {
            SortCriteria sortCriteria = new SortCriteria();
            sortCriteria.setSortBy(GlobalSearchSortCategory.createdDate.toString());
            sortCriteria.setSortDirection(GlobalSearchSortDirection.ASCENDING.toString());
            this.setSortCriteria(sortCriteria);

        } else if (this.getSortCriteria().getSortBy() == null) {
            this.getSortCriteria().setSortBy(GlobalSearchSortCategory.createdDate.toString());

        } else if (this.getSortCriteria().getSortDirection() == null) {
            this.getSortCriteria().setSortDirection(GlobalSearchSortDirection.ASCENDING.toString());
        }
        return this;
    }
}
