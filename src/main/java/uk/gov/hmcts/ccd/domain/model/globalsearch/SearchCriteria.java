package uk.gov.hmcts.ccd.domain.model.globalsearch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.UpperCamelCaseStrategy.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchCriteria {
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<OtherCaseReference> otherCaseReferences;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<SearchParty> searchParties;

    @JsonIgnore
    public boolean isEmpty() {
        return (otherCaseReferences == null || otherCaseReferences.isEmpty())
            && (searchParties == null || searchParties.isEmpty());
    }
}
