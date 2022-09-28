package uk.gov.hmcts.ccd.domain.model.search.global;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.model.std.validator.globalsearch.ValidSearchCriteria;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@Getter
@Setter
public class GlobalSearchRequestPayload {

    @Range(max = 10000, min = 1, message = ValidationError.MAX_RECORD_COUNT_INVALID)
    private Integer maxReturnRecordCount;

    @Min(value = 1, message = ValidationError.START_RECORD_NUMBER_INVALID)
    private Integer startRecordNumber;

    @Valid
    private List<SortCriteria> sortCriteria;

    @ValidSearchCriteria
    @Valid
    private SearchCriteria searchCriteria;

    @JsonIgnore
    public void setDefaults() {

        if (this.getMaxReturnRecordCount() == null) {
            this.setMaxReturnRecordCount(25);
        }
        if (this.getStartRecordNumber() == null) {
            this.setStartRecordNumber(1);
        }
        if (this.getSortCriteria() == null) {
            SortCriteria criteria = new SortCriteria();
            criteria.setSortBy(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName());
            criteria.setSortDirection(GlobalSearchSortDirection.ASCENDING.name());
            List<SortCriteria> sortCriteriaList = List.of(criteria);
            this.setSortCriteria(sortCriteriaList);

        } else {
            for (SortCriteria criteria : this.getSortCriteria()) {
                if (criteria.getSortBy() == null) {
                    criteria.setSortBy(GlobalSearchSortByCategory.CREATED_DATE.getCategoryName());

                } else if (criteria.getSortDirection() == null) {
                    criteria.setSortDirection(GlobalSearchSortDirection.ASCENDING.name());
                }
            }
        }
    }
}
