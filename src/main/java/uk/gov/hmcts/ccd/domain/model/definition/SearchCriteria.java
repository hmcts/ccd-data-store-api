package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class SearchCriteria implements Serializable, Copyable<SearchCriteria> {

    private String caseTypeId;
    private String otherCaseReference;
    private Date liveFrom;
    private Date liveTo;

    @JsonIgnore
    @Override
    public SearchCriteria createCopy() {
        SearchCriteria clonedCriteria = new SearchCriteria();
        clonedCriteria.setCaseTypeId(this.caseTypeId);
        clonedCriteria.setOtherCaseReference(this.otherCaseReference);
        clonedCriteria.setLiveFrom(this.liveFrom != null ? new Date(this.liveFrom.getTime()) : null);
        clonedCriteria.setLiveTo(this.liveTo != null ? new Date(this.liveTo.getTime()) : null);
        return clonedCriteria;
    }
}
